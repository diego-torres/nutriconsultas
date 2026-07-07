package com.nutriconsultas.subscription.lifecycle;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.nutriconsultas.platform.PlatformAdminService;
import com.nutriconsultas.subscription.SubscriptionErrorResponses;
import com.nutriconsultas.subscription.SubscriptionProperties;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SubscriptionAccessInterceptor implements HandlerInterceptor {

	private static final Locale SPANISH_LOCALE = Locale.forLanguageTag("es-MX");

	private final SubscriptionAccessService subscriptionAccessService;

	private final PlatformAdminService platformAdminService;

	private final SubscriptionProperties subscriptionProperties;

	private final MessageSource messageSource;

	public SubscriptionAccessInterceptor(final SubscriptionAccessService subscriptionAccessService,
			final PlatformAdminService platformAdminService, final SubscriptionProperties subscriptionProperties,
			final MessageSource messageSource) {
		this.subscriptionAccessService = subscriptionAccessService;
		this.platformAdminService = platformAdminService;
		this.subscriptionProperties = subscriptionProperties;
		this.messageSource = messageSource;
	}

	@Override
	public boolean preHandle(@NonNull final HttpServletRequest request, @NonNull final HttpServletResponse response,
			@NonNull final Object handler) throws IOException {
		if (!subscriptionProperties.isEnforceNutritionistAccess()) {
			return true;
		}
		final String requestUri = request.getRequestURI();
		if (!requiresNutritionistSubscription(requestUri)) {
			return true;
		}
		if (isAllowedWithoutSubscription(requestUri)) {
			return true;
		}
		final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof OidcUser principal)) {
			return true;
		}
		if (platformAdminService.isPlatformAdmin(principal)) {
			return true;
		}
		if (!subscriptionAccessService.isAdminAccessBlocked(principal.getSubject())) {
			return true;
		}
		if (requestUri.startsWith("/rest/")) {
			writeRestAccessDenied(response);
			return false;
		}
		response.sendRedirect(resolveBlockedRedirect(request, principal.getSubject()));
		return false;
	}

	private String resolveBlockedRedirect(final HttpServletRequest request, final String userId) {
		if (subscriptionAccessService.findSubscriptionForUser(userId).isPresent()) {
			return request.getContextPath() + "/admin/subscription/billing";
		}
		return request.getContextPath() + "/admin/subscription/access-denied";
	}

	private void writeRestAccessDenied(final HttpServletResponse response) throws IOException {
		final String message = messageSource.getMessage(SubscriptionErrorResponses.KEY_INVITATION_REQUIRED, null,
				SPANISH_LOCALE);
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType("application/json");
		response.getWriter().write("{\"error\":\"invitation_required\",\"message\":\"" + escapeJson(message) + "\"}");
	}

	private static String escapeJson(final String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	private static boolean requiresNutritionistSubscription(final String uri) {
		if ("/admin".equals(uri) || uri.startsWith("/admin/")) {
			return true;
		}
		if (!uri.startsWith("/rest/")) {
			return false;
		}
		if (uri.startsWith("/rest/mobile/")) {
			return false;
		}
		return !uri.startsWith("/rest/subscription/payment/webhook") && !uri.startsWith("/rest/public/booking/")
				&& !uri.startsWith("/rest/webhooks/apple/sign-in");
	}

	private static boolean isAllowedWithoutSubscription(final String uri) {
		return uri.startsWith("/admin/subscription/billing") || uri.startsWith("/admin/subscription/access-denied")
				|| uri.startsWith("/admin/platform");
	}

}
