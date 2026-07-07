package com.nutriconsultas.auth0;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import lombok.extern.slf4j.Slf4j;

@Component
@ConditionalOnBean(Auth0ManagementTokenProvider.class)
@Slf4j
public class Auth0ManagementUserServiceImpl implements Auth0ManagementUserService {

	private static final ParameterizedTypeReference<List<Map<String, Object>>> USER_LIST_TYPE = new ParameterizedTypeReference<>() {
	};

	private static final ParameterizedTypeReference<Map<String, Object>> USER_MAP_TYPE = new ParameterizedTypeReference<>() {
	};

	private final RestClient restClient;

	private final Auth0ManagementTokenProvider tokenProvider;

	private final boolean userDeleteEnabled;

	private final int maxRetries;

	public Auth0ManagementUserServiceImpl(final RestClient.Builder restClientBuilder,
			final Auth0ManagementTokenProvider tokenProvider,
			@Value("${app.auth0.management.user-delete-enabled:false}") final boolean userDeleteEnabled,
			@Value("${app.auth0.management.max-retries:2}") final int maxRetries) {
		this.restClient = restClientBuilder.build();
		this.tokenProvider = tokenProvider;
		this.userDeleteEnabled = userDeleteEnabled;
		this.maxRetries = Math.max(0, maxRetries);
	}

	@Override
	public boolean isConfigured() {
		return tokenProvider.isConfigured();
	}

	@Override
	public Optional<Auth0ManagementUser> findUserByAppleSubject(final String appleSubject) {
		if (!StringUtils.hasText(appleSubject)) {
			return Optional.empty();
		}
		final String normalizedSubject = appleSubject.trim();
		final String auth0UserId = Auth0AppleIdentitySupport.toAuth0UserId(normalizedSubject);
		final Optional<Auth0ManagementUser> direct = findUserById(auth0UserId);
		if (direct.isPresent()) {
			return direct;
		}
		final String luceneQuery = "identities.provider:\"apple\" AND identities.user_id:\""
				+ escapeLuceneValue(normalizedSubject) + "\"";
		final List<Auth0ManagementUser> matches = searchUsers(luceneQuery);
		if (matches.isEmpty()) {
			return Optional.empty();
		}
		if (matches.size() > 1 && log.isWarnEnabled()) {
			log.warn("Ambiguous Auth0 Apple subject lookup: subjectHash={}, matchCount={}",
					normalizedSubject.hashCode(), matches.size());
		}
		return Optional.of(matches.get(0));
	}

	@Override
	public List<Auth0ManagementUser> searchUsersByEmail(final String email) {
		if (!StringUtils.hasText(email)) {
			return List.of();
		}
		final String luceneQuery = "email:\"" + escapeLuceneValue(email.trim().toLowerCase()) + "\"";
		return searchUsers(luceneQuery);
	}

	@Override
	public void updateAppMetadata(final String auth0UserId, final Map<String, Object> appMetadataPatch) {
		requireConfiguredUserId(auth0UserId);
		if (appMetadataPatch == null || appMetadataPatch.isEmpty()) {
			throw new IllegalArgumentException("appMetadataPatch is required");
		}
		executeVoidWithRetry("update app_metadata", () -> {
			final String token = tokenProvider.obtainToken();
			restClient.patch()
				.uri("https://{domain}/api/v2/users/{userId}", tokenProvider.getDomain(), auth0UserId)
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("app_metadata", appMetadataPatch))
				.retrieve()
				.toBodilessEntity();
		});
	}

	@Override
	public void blockUserInAppMetadata(final String auth0UserId) {
		updateAppMetadata(auth0UserId,
				Map.of("apple_signin_blocked", true, "apple_signin_blocked_at", Instant.now().toString()));
	}

	@Override
	public void deleteUser(final String auth0UserId) {
		requireConfiguredUserId(auth0UserId);
		if (!userDeleteEnabled) {
			throw new IllegalStateException("Auth0 user deletion is disabled by configuration");
		}
		executeVoidWithRetry("delete user", () -> {
			final String token = tokenProvider.obtainToken();
			restClient.delete()
				.uri("https://{domain}/api/v2/users/{userId}", tokenProvider.getDomain(), auth0UserId)
				.header("Authorization", "Bearer " + token)
				.retrieve()
				.toBodilessEntity();
		});
	}

	private void requireConfiguredUserId(final String auth0UserId) {
		if (!isConfigured()) {
			throw new IllegalStateException("Auth0 Management API is not configured");
		}
		if (!StringUtils.hasText(auth0UserId)) {
			throw new IllegalArgumentException("auth0UserId is required");
		}
	}

	private Optional<Auth0ManagementUser> findUserById(final String auth0UserId) {
		if (!isConfigured() || !StringUtils.hasText(auth0UserId)) {
			return Optional.empty();
		}
		try {
			return Optional.ofNullable(executeWithRetry("get user", () -> {
				final String token = tokenProvider.obtainToken();
				final Map<String, Object> user = restClient.get()
					.uri("https://{domain}/api/v2/users/{userId}", tokenProvider.getDomain(), auth0UserId)
					.header("Authorization", "Bearer " + token)
					.accept(MediaType.APPLICATION_JSON)
					.retrieve()
					.body(USER_MAP_TYPE);
				return Auth0ManagementUser.fromApiMap(user);
			}));
		}
		catch (Auth0ManagementApiException ex) {
			if (log.isDebugEnabled()) {
				log.debug("Auth0 user lookup by id missed for userIdHash={}", auth0UserId.hashCode());
			}
			return Optional.empty();
		}
	}

	private List<Auth0ManagementUser> searchUsers(final String luceneQuery) {
		if (!isConfigured()) {
			return List.of();
		}
		final List<Map<String, Object>> users = executeWithRetry("search users", () -> {
			final String token = tokenProvider.obtainToken();
			return restClient.get()
				.uri(uriBuilder -> uriBuilder.scheme("https")
					.host(tokenProvider.getDomain())
					.path("/api/v2/users")
					.queryParam("q", luceneQuery)
					.queryParam("search_engine", "v3")
					.build())
				.header("Authorization", "Bearer " + token)
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.body(USER_LIST_TYPE);
		});
		if (users == null || users.isEmpty()) {
			return List.of();
		}
		final List<Auth0ManagementUser> results = new ArrayList<>();
		for (final Map<String, Object> user : users) {
			final Auth0ManagementUser mapped = Auth0ManagementUser.fromApiMap(user);
			if (mapped != null && StringUtils.hasText(mapped.userId())) {
				results.add(mapped);
			}
		}
		return results;
	}

	private void executeVoidWithRetry(final String operation, final Runnable operationRunnable) {
		RuntimeException lastFailure = null;
		for (int attempt = 0; attempt <= maxRetries; attempt++) {
			try {
				operationRunnable.run();
				return;
			}
			catch (RestClientException ex) {
				lastFailure = new Auth0ManagementApiException("Auth0 Management API " + operation + " failed", ex);
				if (attempt < maxRetries && log.isWarnEnabled()) {
					log.warn("Retrying Auth0 Management API {} attempt={}", operation, attempt + 1);
				}
			}
		}
		throw lastFailure;
	}

	private <T> T executeWithRetry(final String operation, final RetryableOperation<T> operationSupplier) {
		RuntimeException lastFailure = null;
		for (int attempt = 0; attempt <= maxRetries; attempt++) {
			try {
				return operationSupplier.execute();
			}
			catch (RestClientException ex) {
				lastFailure = new Auth0ManagementApiException("Auth0 Management API " + operation + " failed", ex);
				if (attempt < maxRetries && log.isWarnEnabled()) {
					log.warn("Retrying Auth0 Management API {} attempt={}", operation, attempt + 1);
				}
			}
		}
		throw lastFailure;
	}

	private static String escapeLuceneValue(final String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	@FunctionalInterface
	private interface RetryableOperation<T> {

		T execute();

	}

}
