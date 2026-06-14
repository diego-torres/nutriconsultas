package com.nutriconsultas.auth0;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.extern.slf4j.Slf4j;

@Component
@ConditionalOnExpression("'${app.auth0.management.client-id:}'.length() > 0")
@Slf4j
public class Auth0UserLookupImpl implements Auth0UserLookup {

	private static final ParameterizedTypeReference<List<Map<String, Object>>> USER_LIST_TYPE = new ParameterizedTypeReference<>() {
	};

	private final RestClient restClient;

	private final String clientId;

	private final String clientSecret;

	private final String domain;

	private volatile CachedToken cachedToken;

	public Auth0UserLookupImpl(final RestClient.Builder restClientBuilder,
			@Value("${app.auth0.management.client-id}") final String clientId,
			@Value("${app.auth0.management.client-secret}") final String clientSecret,
			@Value("${app.auth0.management.domain}") final String domain,
			@Value("${AUTH_ISSUER:}") final String authIssuer) {
		this.restClient = restClientBuilder.build();
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.domain = normalizeDomain(StringUtils.hasText(domain) ? domain : authIssuer);
	}

	@Override
	public boolean isConfigured() {
		return StringUtils.hasText(clientId) && StringUtils.hasText(clientSecret) && StringUtils.hasText(domain);
	}

	@Override
	public Optional<String> findUserIdByEmail(final String email) {
		if (!isConfigured()) {
			return Optional.empty();
		}
		if (!StringUtils.hasText(email)) {
			return Optional.empty();
		}
		final String token = obtainManagementToken();
		final URI uri = UriComponentsBuilder.fromHttpUrl("https://" + domain + "/api/v2/users-by-email")
			.queryParam("email", email.trim().toLowerCase())
			.build(true)
			.toUri();
		final List<Map<String, Object>> users = restClient.get()
			.uri(uri)
			.header("Authorization", "Bearer " + token)
			.accept(MediaType.APPLICATION_JSON)
			.retrieve()
			.body(USER_LIST_TYPE);
		if (users == null || users.isEmpty()) {
			return Optional.empty();
		}
		final Object userId = users.get(0).get("user_id");
		if (userId == null) {
			return Optional.empty();
		}
		return Optional.of(userId.toString());
	}

	private String obtainManagementToken() {
		final CachedToken current = cachedToken;
		if (current != null && current.isValid()) {
			return current.token();
		}
		synchronized (this) {
			final CachedToken again = cachedToken;
			if (again != null && again.isValid()) {
				return again.token();
			}
			final Map<String, Object> response = restClient.post()
				.uri("https://" + domain + "/oauth/token")
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("client_id", clientId, "client_secret", clientSecret, "audience",
						"https://" + domain + "/api/v2/", "grant_type", "client_credentials"))
				.retrieve()
				.body(new ParameterizedTypeReference<Map<String, Object>>() {
				});
			if (response == null || response.get("access_token") == null) {
				throw new IllegalStateException("Auth0 Management API token request failed");
			}
			final String accessToken = response.get("access_token").toString();
			final long expiresIn = response.get("expires_in") instanceof Number number ? number.longValue() : 3600L;
			cachedToken = new CachedToken(accessToken, Instant.now().plusSeconds(Math.max(60L, expiresIn - 60L)));
			if (log.isDebugEnabled()) {
				log.debug("Obtained Auth0 Management API token for domain {}", domain);
			}
			return accessToken;
		}
	}

	private static String normalizeDomain(final String rawDomain) {
		if (!StringUtils.hasText(rawDomain)) {
			return "";
		}
		String normalized = rawDomain.trim();
		if (normalized.startsWith("https://")) {
			normalized = normalized.substring("https://".length());
		}
		else if (normalized.startsWith("http://")) {
			normalized = normalized.substring("http://".length());
		}
		while (normalized.endsWith("/")) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}
		return normalized;
	}

	private record CachedToken(String token, Instant expiresAt) {

		boolean isValid() {
			return Instant.now().isBefore(expiresAt);
		}

	}

}
