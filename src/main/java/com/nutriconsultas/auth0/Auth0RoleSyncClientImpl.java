package com.nutriconsultas.auth0;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.nutriconsultas.subscription.PlanTier;

import lombok.extern.slf4j.Slf4j;

@Component
@ConditionalOnBean(Auth0ManagementTokenProvider.class)
@Slf4j
public class Auth0RoleSyncClientImpl implements Auth0RoleSyncClient {

	private static final ParameterizedTypeReference<List<Map<String, Object>>> ROLE_LIST_TYPE = new ParameterizedTypeReference<>() {
	};

	private final RestClient restClient;

	private final Auth0ManagementTokenProvider tokenProvider;

	private final Map<String, String> roleIdBySlug = new ConcurrentHashMap<>();

	public Auth0RoleSyncClientImpl(final RestClient.Builder restClientBuilder,
			final Auth0ManagementTokenProvider tokenProvider) {
		this.restClient = restClientBuilder.build();
		this.tokenProvider = tokenProvider;
	}

	@Override
	public boolean isConfigured() {
		return tokenProvider.isConfigured();
	}

	@Override
	public void syncPlanRole(final String auth0UserId, final PlanTier planTier) {
		if (!isConfigured()) {
			throw new IllegalStateException("Auth0 Management API is not configured");
		}
		if (!StringUtils.hasText(auth0UserId)) {
			throw new IllegalArgumentException("auth0UserId is required");
		}
		if (planTier == null) {
			throw new IllegalArgumentException("planTier is required");
		}
		final String token = tokenProvider.obtainToken();
		final String encodedUserId = URLEncoder.encode(auth0UserId, StandardCharsets.UTF_8);
		for (final PlanTier tier : PlanTier.values()) {
			removeRoleIfPresent(token, encodedUserId, tier.getRoleSlug());
		}
		assignRole(token, encodedUserId, planTier.getRoleSlug());
		if (log.isInfoEnabled()) {
			log.info("Synced Auth0 plan role: userId={}, roleSlug={}", auth0UserId, planTier.getRoleSlug());
		}
	}

	private void removeRoleIfPresent(final String token, final String encodedUserId, final String roleSlug) {
		final String roleId = resolveRoleId(token, roleSlug);
		if (roleId == null) {
			return;
		}
		restClient.method(org.springframework.http.HttpMethod.DELETE)
			.uri("https://" + tokenProvider.getDomain() + "/api/v2/users/" + encodedUserId + "/roles")
			.header("Authorization", "Bearer " + token)
			.contentType(MediaType.APPLICATION_JSON)
			.body(List.of(roleId))
			.retrieve()
			.toBodilessEntity();
	}

	private void assignRole(final String token, final String encodedUserId, final String roleSlug) {
		final String roleId = resolveRoleId(token, roleSlug);
		if (roleId == null) {
			throw new IllegalStateException("Auth0 role not found for slug: " + roleSlug);
		}
		restClient.post()
			.uri("https://" + tokenProvider.getDomain() + "/api/v2/users/" + encodedUserId + "/roles")
			.header("Authorization", "Bearer " + token)
			.contentType(MediaType.APPLICATION_JSON)
			.body(List.of(roleId))
			.retrieve()
			.toBodilessEntity();
	}

	private String resolveRoleId(final String token, final String roleSlug) {
		return roleIdBySlug.computeIfAbsent(roleSlug, slug -> lookupRoleId(token, slug));
	}

	private String lookupRoleId(final String token, final String roleSlug) {
		final String encodedFilter = URLEncoder.encode(roleSlug, StandardCharsets.UTF_8);
		final List<Map<String, Object>> roles = restClient.get()
			.uri("https://" + tokenProvider.getDomain() + "/api/v2/roles?name_filter=" + encodedFilter)
			.header("Authorization", "Bearer " + token)
			.accept(MediaType.APPLICATION_JSON)
			.retrieve()
			.body(ROLE_LIST_TYPE);
		if (roles == null || roles.isEmpty()) {
			return null;
		}
		for (final Map<String, Object> role : roles) {
			if (roleSlug.equals(role.get("name"))) {
				final Object roleId = role.get("id");
				return roleId == null ? null : roleId.toString();
			}
		}
		return null;
	}

}
