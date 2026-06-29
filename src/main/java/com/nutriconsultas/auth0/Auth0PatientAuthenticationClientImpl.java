package com.nutriconsultas.auth0;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import lombok.extern.slf4j.Slf4j;

@Component
@ConditionalOnExpression("'${app.auth0.patient-broker.broker-client-id:}'.length() > 0")
@Slf4j
public class Auth0PatientAuthenticationClientImpl implements Auth0PatientAuthenticationClient {

	private static final String PASSWORD_REALM_GRANT = "http://auth0.com/oauth/grant-type/password-realm";

	private static final String DEFAULT_SCOPE = "openid profile email offline_access";

	private final RestClient restClient;

	private final Auth0PatientAuthenticationProperties properties;

	public Auth0PatientAuthenticationClientImpl(final RestClient.Builder restClientBuilder,
			final Auth0PatientAuthenticationProperties properties) {
		this.restClient = restClientBuilder.build();
		this.properties = properties;
	}

	@Override
	public boolean isConfigured() {
		return StringUtils.hasText(properties.getBrokerClientId())
				&& StringUtils.hasText(properties.getBrokerClientSecret())
				&& StringUtils.hasText(properties.getNativeClientId()) && StringUtils.hasText(resolveDomain())
				&& StringUtils.hasText(properties.getAudience());
	}

	@Override
	public void signUpDatabaseUser(final String email, final String password, final Map<String, String> userMetadata) {
		assertConfigured();
		final Map<String, Object> body = new HashMap<>();
		body.put("client_id", properties.getNativeClientId());
		body.put("email", email);
		body.put("password", password);
		body.put("connection", properties.getDatabaseConnection());
		if (userMetadata != null && !userMetadata.isEmpty()) {
			body.put("user_metadata", userMetadata);
		}
		try {
			restClient.post()
				.uri("https://" + resolveDomain() + "/dbconnections/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.retrieve()
				.toBodilessEntity();
		}
		catch (final RestClientResponseException ex) {
			throw mapAuth0Failure(ex);
		}
		if (log.isDebugEnabled()) {
			log.debug("Auth0 database signup completed for broker flow");
		}
	}

	@Override
	public Auth0PatientTokenResponse loginWithPassword(final String email, final String password,
			final String invitationToken) {
		assertConfigured();
		final Map<String, Object> body = new HashMap<>();
		body.put("grant_type", PASSWORD_REALM_GRANT);
		body.put("client_id", properties.getBrokerClientId());
		body.put("client_secret", properties.getBrokerClientSecret());
		body.put("username", email);
		body.put("password", password);
		body.put("realm", properties.getDatabaseConnection());
		body.put("audience", properties.getAudience());
		body.put("scope", DEFAULT_SCOPE);
		if (StringUtils.hasText(invitationToken)) {
			body.put("invitation_token", invitationToken.trim());
		}
		try {
			final Map<String, Object> response = restClient.post()
				.uri("https://" + resolveDomain() + "/oauth/token")
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.retrieve()
				.body(new ParameterizedTypeReference<Map<String, Object>>() {
				});
			return toTokenResponse(response);
		}
		catch (final RestClientResponseException ex) {
			throw mapAuth0Failure(ex);
		}
	}

	private Auth0PatientTokenResponse toTokenResponse(final Map<String, Object> response) {
		if (response == null || response.get("access_token") == null || response.get("id_token") == null) {
			throw new Auth0PatientAuthenticationException(HttpStatus.BAD_GATEWAY,
					Auth0PatientAuthenticationException.CODE_AUTH0_ERROR, "Auth0 token response incomplete");
		}
		final long expiresIn = response.get("expires_in") instanceof Number number ? number.longValue() : 3600L;
		final String refreshToken = response.get("refresh_token") instanceof String token ? token : null;
		final String tokenType = response.get("token_type") instanceof String type ? type : "Bearer";
		return new Auth0PatientTokenResponse(response.get("access_token").toString(),
				response.get("id_token").toString(), refreshToken, expiresIn, tokenType);
	}

	private Auth0PatientAuthenticationException mapAuth0Failure(final RestClientResponseException ex) {
		final String description = extractErrorDescription(ex);
		final String normalized = description.toLowerCase();
		if (ex.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()
				|| ex.getStatusCode().value() == HttpStatus.FORBIDDEN.value()) {
			if (normalized.contains("invitation") || normalized.contains("invitación")) {
				return new Auth0PatientAuthenticationException(HttpStatus.FORBIDDEN,
						Auth0PatientAuthenticationException.CODE_INVITATION_REQUIRED, description);
			}
			return new Auth0PatientAuthenticationException(HttpStatus.UNAUTHORIZED,
					Auth0PatientAuthenticationException.CODE_INVALID_CREDENTIALS, description);
		}
		if (ex.getStatusCode().value() == HttpStatus.BAD_REQUEST.value()) {
			if (normalized.contains("exists") || normalized.contains("already")) {
				return new Auth0PatientAuthenticationException(HttpStatus.CONFLICT,
						Auth0PatientAuthenticationException.CODE_EMAIL_IN_USE, description);
			}
			if (normalized.contains("password")) {
				return new Auth0PatientAuthenticationException(HttpStatus.BAD_REQUEST,
						Auth0PatientAuthenticationException.CODE_WEAK_PASSWORD, description);
			}
		}
		if (log.isDebugEnabled()) {
			log.debug("Auth0 patient authentication failed with status={}", ex.getStatusCode().value());
		}
		return new Auth0PatientAuthenticationException(HttpStatus.BAD_GATEWAY,
				Auth0PatientAuthenticationException.CODE_AUTH0_ERROR, description);
	}

	private String extractErrorDescription(final RestClientResponseException ex) {
		final String body = ex.getResponseBodyAsString();
		if (!StringUtils.hasText(body)) {
			return "Authentication failed";
		}
		if (body.contains("error_description")) {
			final int start = body.indexOf("error_description");
			final int colon = body.indexOf(':', start);
			final int quoteStart = body.indexOf('"', colon + 1);
			final int quoteEnd = body.indexOf('"', quoteStart + 1);
			if (quoteStart >= 0 && quoteEnd > quoteStart) {
				return body.substring(quoteStart + 1, quoteEnd);
			}
		}
		return "Authentication failed";
	}

	private void assertConfigured() {
		if (!isConfigured()) {
			throw new IllegalStateException("Auth0 patient authentication broker is not configured");
		}
	}

	private String resolveDomain() {
		return Auth0DomainNormalizer.normalize(properties.getDomain());
	}

}
