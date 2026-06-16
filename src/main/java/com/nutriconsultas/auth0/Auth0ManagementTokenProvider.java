package com.nutriconsultas.auth0;

import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import lombok.extern.slf4j.Slf4j;

/**
 * Obtains and caches Auth0 Management API access tokens for M2M clients.
 */
@Component
@ConditionalOnExpression("'${app.auth0.management.client-id:}'.length() > 0")
@Slf4j
public class Auth0ManagementTokenProvider {

	private final RestClient restClient;

	private final String clientId;

	private final String clientSecret;

	private final String domain;

	private volatile CachedToken cachedToken;

	public Auth0ManagementTokenProvider(final RestClient.Builder restClientBuilder,
			@Value("${app.auth0.management.client-id}") final String clientId,
			@Value("${app.auth0.management.client-secret}") final String clientSecret,
			@Value("${app.auth0.management.domain}") final String domain,
			@Value("${AUTH_ISSUER:}") final String authIssuer) {
		this.restClient = restClientBuilder.build();
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.domain = Auth0DomainNormalizer.normalize(StringUtils.hasText(domain) ? domain : authIssuer);
	}

	public boolean isConfigured() {
		return StringUtils.hasText(clientId) && StringUtils.hasText(clientSecret) && StringUtils.hasText(domain);
	}

	public String getDomain() {
		return domain;
	}

	public String obtainToken() {
		if (!isConfigured()) {
			throw new IllegalStateException("Auth0 Management API is not configured");
		}
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

	private record CachedToken(String token, Instant expiresAt) {

		boolean isValid() {
			return Instant.now().isBefore(expiresAt);
		}

	}

}
