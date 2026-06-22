package com.nutriconsultas.security;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * After Auth0 login, returns invited users to the redeem page when a pending invitation
 * token was stored in session.
 */
@Component
public class NutritionistOAuth2LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

	public static final String PENDING_INVITATION_TOKEN_SESSION_KEY = "PENDING_NUTRITIONIST_INVITATION_TOKEN";

	@PostConstruct
	void init() {
		setDefaultTargetUrl("/admin");
		setAlwaysUseDefaultTargetUrl(false);
	}

	@Override
	public void onAuthenticationSuccess(final HttpServletRequest request, final HttpServletResponse response,
			final Authentication authentication) throws IOException, ServletException {
		final HttpSession session = request.getSession(false);
		if (session != null) {
			final Object pendingToken = session.getAttribute(PENDING_INVITATION_TOKEN_SESSION_KEY);
			if (pendingToken instanceof String token && !token.isBlank()) {
				session.removeAttribute(PENDING_INVITATION_TOKEN_SESSION_KEY);
				final String redeemUrl = request.getContextPath() + "/invitation/nutritionist/redeem?token="
						+ URLEncoder.encode(token, StandardCharsets.UTF_8);
				getRedirectStrategy().sendRedirect(request, response, redeemUrl);
				return;
			}
		}
		super.onAuthenticationSuccess(request, response, authentication);
	}

}
