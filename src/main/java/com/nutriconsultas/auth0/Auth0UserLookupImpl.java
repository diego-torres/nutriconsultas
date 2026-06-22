package com.nutriconsultas.auth0;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.extern.slf4j.Slf4j;

@Component
@ConditionalOnBean(Auth0ManagementTokenProvider.class)
@Slf4j
public class Auth0UserLookupImpl implements Auth0UserLookup {

	private static final ParameterizedTypeReference<List<Map<String, Object>>> USER_LIST_TYPE = new ParameterizedTypeReference<>() {
	};

	private static final ParameterizedTypeReference<Map<String, Object>> USER_TYPE = new ParameterizedTypeReference<>() {
	};

	private final RestClient restClient;

	private final Auth0ManagementTokenProvider tokenProvider;

	public Auth0UserLookupImpl(final RestClient.Builder restClientBuilder,
			final Auth0ManagementTokenProvider tokenProvider) {
		this.restClient = restClientBuilder.build();
		this.tokenProvider = tokenProvider;
	}

	@Override
	public boolean isConfigured() {
		return tokenProvider.isConfigured();
	}

	@Override
	public Optional<String> findUserIdByEmail(final String email) {
		if (!isConfigured()) {
			return Optional.empty();
		}
		if (!StringUtils.hasText(email)) {
			return Optional.empty();
		}
		final String token = tokenProvider.obtainToken();
		final URI uri = UriComponentsBuilder
			.fromHttpUrl("https://" + tokenProvider.getDomain() + "/api/v2/users-by-email")
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

	@Override
	public Optional<String> findEmailByUserId(final String userId) {
		if (!isConfigured() || !StringUtils.hasText(userId)) {
			return Optional.empty();
		}
		try {
			final String token = tokenProvider.obtainToken();
			final String encodedUserId = URLEncoder.encode(userId, StandardCharsets.UTF_8);
			final Map<String, Object> user = restClient.get()
				.uri("https://" + tokenProvider.getDomain() + "/api/v2/users/" + encodedUserId)
				.header("Authorization", "Bearer " + token)
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.body(USER_TYPE);
			if (user == null) {
				return Optional.empty();
			}
			final Object email = user.get("email");
			if (email == null || !StringUtils.hasText(email.toString())) {
				return Optional.empty();
			}
			return Optional.of(email.toString().trim());
		}
		catch (RuntimeException ex) {
			if (log.isDebugEnabled()) {
				log.debug("Auth0 user email lookup failed for userId present={}", StringUtils.hasText(userId), ex);
			}
			return Optional.empty();
		}
	}

}
