package com.nutriconsultas.security;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Auth0 login uses {@code screen_hint=login} by default so public entry cannot
 * self-register. Invitation redeem links pass {@code signup=true} to allow account
 * creation.
 */
@Component
public class NutritionistOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

	private static final String SCREEN_HINT = "screen_hint";

	private static final String SIGNUP_PARAMETER = "signup";

	private final OAuth2AuthorizationRequestResolver delegate;

	public NutritionistOAuth2AuthorizationRequestResolver(
			final ClientRegistrationRepository clientRegistrationRepository) {
		this.delegate = new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository,
				"/oauth2/authorization");
	}

	@Override
	public OAuth2AuthorizationRequest resolve(final HttpServletRequest request) {
		return customize(delegate.resolve(request), request);
	}

	@Override
	public OAuth2AuthorizationRequest resolve(final HttpServletRequest request, final String clientRegistrationId) {
		return customize(delegate.resolve(request, clientRegistrationId), request);
	}

	private OAuth2AuthorizationRequest customize(final OAuth2AuthorizationRequest authorizationRequest,
			final HttpServletRequest request) {
		if (authorizationRequest == null) {
			return null;
		}
		final Map<String, Object> additionalParameters = new HashMap<>(authorizationRequest.getAdditionalParameters());
		if ("true".equalsIgnoreCase(request.getParameter(SIGNUP_PARAMETER))) {
			additionalParameters.put(SCREEN_HINT, "signup");
		}
		else {
			additionalParameters.put(SCREEN_HINT, "login");
		}
		return OAuth2AuthorizationRequest.from(authorizationRequest).additionalParameters(additionalParameters).build();
	}

}
