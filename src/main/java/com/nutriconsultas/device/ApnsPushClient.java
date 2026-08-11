package com.nutriconsultas.device;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

/**
 * APNs HTTP/2 provider API client using a .p8 auth key (#575).
 */
@Component
@Slf4j
public class ApnsPushClient {

	private static final Set<String> INVALID_REASONS = Set.of("BadDeviceToken", "Unregistered", "ExpiredToken",
			"DeviceTokenNotForTopic");

	private final PatientPushProperties properties;

	private final ObjectMapper objectMapper;

	private final HttpClient httpClient;

	private final AtomicReference<CachedJwt> cachedJwt = new AtomicReference<>();

	@Autowired
	public ApnsPushClient(final PatientPushProperties properties, final ObjectMapper objectMapper) {
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.httpClient = HttpClient.newBuilder()
			.version(HttpClient.Version.HTTP_2)
			.connectTimeout(Duration.ofMillis(properties.getApns().getConnectTimeoutMs()))
			.build();
	}

	ApnsPushClient(final PatientPushProperties properties, final ObjectMapper objectMapper,
			final HttpClient httpClient) {
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.httpClient = httpClient;
	}

	public PushDeliveryResult send(final PatientDevice device, final PushEvent event) {
		if (!properties.isApnsConfigured()) {
			return PushDeliveryResult.SKIPPED;
		}
		try {
			final String bearer = providerToken();
			final String body = objectMapper.writeValueAsString(buildPayload(event));
			final HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://" + properties.getApns().resolveHost() + "/3/device/" + device.getToken()))
				.timeout(Duration.ofMillis(properties.getApns().getReadTimeoutMs()))
				.header("authorization", "bearer " + bearer)
				.header("apns-topic", properties.getApns().getBundleId())
				.header("apns-push-type", "alert")
				.header("apns-priority", "10")
				.header("content-type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();
			final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			return mapResponse(response.statusCode(), response.body(), device.getId());
		}
		catch (final InterruptedException ex) {
			Thread.currentThread().interrupt();
			log.warn("APNs send interrupted for deviceId={}", device.getId());
			return PushDeliveryResult.TRANSIENT_FAILURE;
		}
		catch (final Exception ex) {
			log.warn("APNs send failed for deviceId={}: {}", device.getId(), ex.getMessage());
			return PushDeliveryResult.TRANSIENT_FAILURE;
		}
	}

	private PushDeliveryResult mapResponse(final int status, final String responseBody, final Long deviceId)
			throws IOException {
		if (status == 200) {
			return PushDeliveryResult.SUCCESS;
		}
		final String reason = extractReason(responseBody);
		if (status == 410 || INVALID_REASONS.contains(reason)) {
			if (log.isInfoEnabled()) {
				log.info("APNs permanent failure for deviceId={} status={} reason={}", deviceId, status, reason);
			}
			return PushDeliveryResult.INVALID_TOKEN;
		}
		log.warn("APNs transient failure for deviceId={} status={} reason={}", deviceId, status, reason);
		return PushDeliveryResult.TRANSIENT_FAILURE;
	}

	private String extractReason(final String responseBody) throws IOException {
		if (responseBody == null || responseBody.isBlank()) {
			return "";
		}
		final JsonNode root = objectMapper.readTree(responseBody);
		final JsonNode reason = root.get("reason");
		return reason != null && !reason.isNull() ? reason.asText("") : "";
	}

	private Map<String, Object> buildPayload(final PushEvent event) {
		final Map<String, Object> alert = new LinkedHashMap<>();
		alert.put("title", PushNotificationCopy.NEW_MESSAGE_TITLE);
		alert.put("body", PushNotificationCopy.NEW_MESSAGE_BODY);
		final Map<String, Object> aps = new LinkedHashMap<>();
		aps.put("alert", alert);
		aps.put("sound", "default");
		final Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("aps", aps);
		payload.put("type", event.type().name());
		if (event.messageId() != null) {
			payload.put("messageId", event.messageId());
		}
		return payload;
	}

	private String providerToken() throws Exception {
		final CachedJwt current = cachedJwt.get();
		final Instant now = Instant.now();
		if (current != null && current.expiresAt().isAfter(now.plusSeconds(60))) {
			return current.token();
		}
		final String token = createProviderToken(now);
		cachedJwt.set(new CachedJwt(token, now.plus(Duration.ofMinutes(50))));
		if (log.isDebugEnabled()) {
			log.debug("Refreshed APNs provider JWT for keyId={}",
					LogRedaction.redactUserId(properties.getApns().getKeyId()));
		}
		return token;
	}

	private String createProviderToken(final Instant now) throws Exception {
		final ECPrivateKey privateKey = loadPrivateKey(properties.getApns().getP8Key());
		final JWTClaimsSet claims = new JWTClaimsSet.Builder().issuer(properties.getApns().getTeamId())
			.issueTime(Date.from(now))
			.build();
		final SignedJWT signedJwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT)
			.keyID(properties.getApns().getKeyId())
			.build(), claims);
		signedJwt.sign(new ECDSASigner(privateKey));
		return signedJwt.serialize();
	}

	static ECPrivateKey loadPrivateKey(final String p8Key) throws Exception {
		final String normalized = p8Key.replace("\\n", "\n");
		final String base64 = normalized.replace("-----BEGIN PRIVATE KEY-----", "")
			.replace("-----END PRIVATE KEY-----", "")
			.replaceAll("\\s", "");
		final byte[] decoded = Base64.getDecoder().decode(base64);
		final PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
		return (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(spec);
	}

	private record CachedJwt(String token, Instant expiresAt) {
	}

}
