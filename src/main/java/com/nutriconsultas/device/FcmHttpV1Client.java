package com.nutriconsultas.device;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

/**
 * FCM HTTP v1 client using a Google service account (#575).
 */
@Component
@Slf4j
public class FcmHttpV1Client {

	private static final String FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

	private static final Set<String> INVALID_ERROR_CODES = Set.of("UNREGISTERED", "NOT_FOUND");

	private final PatientPushProperties properties;

	private final ObjectMapper objectMapper;

	private final RestClient restClient;

	private volatile GoogleCredentials credentials;

	@Autowired
	public FcmHttpV1Client(final PatientPushProperties properties, final ObjectMapper objectMapper) {
		this.properties = properties;
		this.objectMapper = objectMapper;
		final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofMillis(properties.getFcm().getConnectTimeoutMs()));
		requestFactory.setReadTimeout(Duration.ofMillis(properties.getFcm().getReadTimeoutMs()));
		this.restClient = RestClient.builder().requestFactory(requestFactory).build();
	}

	FcmHttpV1Client(final PatientPushProperties properties, final ObjectMapper objectMapper,
			final RestClient restClient) {
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.restClient = restClient;
	}

	public PushDeliveryResult send(final PatientDevice device, final PushEvent event) {
		if (!properties.isFcmConfigured()) {
			return PushDeliveryResult.SKIPPED;
		}
		try {
			final String accessToken = accessToken();
			final String url = "https://fcm.googleapis.com/v1/projects/" + properties.getFcm().getProjectId()
					+ "/messages:send";
			restClient.post()
				.uri(url)
				.contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer " + accessToken)
				.body(Map.of("message", buildMessage(device.getToken(), event)))
				.retrieve()
				.toBodilessEntity();
			return PushDeliveryResult.SUCCESS;
		}
		catch (final RestClientResponseException ex) {
			return mapError(ex, device.getId());
		}
		catch (final Exception ex) {
			log.warn("FCM send failed for deviceId={}: {}", device.getId(), ex.getMessage());
			return PushDeliveryResult.TRANSIENT_FAILURE;
		}
	}

	private PushDeliveryResult mapError(final RestClientResponseException ex, final Long deviceId) {
		final String errorCode = extractErrorCode(ex.getResponseBodyAsString());
		if (ex.getStatusCode().value() == 404 || INVALID_ERROR_CODES.contains(errorCode)) {
			if (log.isInfoEnabled()) {
				log.info("FCM permanent failure for deviceId={} status={} errorCode={}", deviceId,
						ex.getStatusCode().value(), errorCode);
			}
			return PushDeliveryResult.INVALID_TOKEN;
		}
		log.warn("FCM transient failure for deviceId={} status={} errorCode={}", deviceId, ex.getStatusCode().value(),
				errorCode);
		return PushDeliveryResult.TRANSIENT_FAILURE;
	}

	private String extractErrorCode(final String responseBody) {
		if (responseBody == null || responseBody.isBlank()) {
			return "";
		}
		try {
			final JsonNode root = objectMapper.readTree(responseBody);
			final JsonNode error = root.path("error");
			final JsonNode details = error.path("details");
			if (details.isArray()) {
				for (final JsonNode detail : details) {
					final JsonNode errorCode = detail.get("errorCode");
					if (errorCode != null && !errorCode.isNull()) {
						return errorCode.asText("");
					}
				}
			}
			final JsonNode status = error.get("status");
			return status != null && !status.isNull() ? status.asText("") : "";
		}
		catch (final Exception ignored) {
			return "";
		}
	}

	private Map<String, Object> buildMessage(final String token, final PushEvent event) {
		final Map<String, String> notification = new LinkedHashMap<>();
		notification.put("title", PushNotificationCopy.NEW_MESSAGE_TITLE);
		notification.put("body", PushNotificationCopy.NEW_MESSAGE_BODY);
		final Map<String, String> data = new LinkedHashMap<>();
		data.put("type", event.type().name());
		if (event.messageId() != null) {
			data.put("messageId", String.valueOf(event.messageId()));
		}
		final Map<String, Object> android = new LinkedHashMap<>();
		android.put("priority", "HIGH");
		final Map<String, Object> message = new LinkedHashMap<>();
		message.put("token", token);
		message.put("notification", notification);
		message.put("data", data);
		message.put("android", android);
		return message;
	}

	private String accessToken() throws Exception {
		GoogleCredentials current = credentials;
		if (current == null) {
			synchronized (this) {
				current = credentials;
				if (current == null) {
					current = GoogleCredentials
						.fromStream(new ByteArrayInputStream(
								properties.getFcm().getServiceAccountJson().getBytes(StandardCharsets.UTF_8)))
						.createScoped(FCM_SCOPE);
					credentials = current;
					if (log.isDebugEnabled()) {
						log.debug("Loaded FCM service account credentials for projectId={}",
								LogRedaction.redactUserId(properties.getFcm().getProjectId()));
					}
				}
			}
		}
		current.refreshIfExpired();
		return current.getAccessToken().getTokenValue();
	}

}
