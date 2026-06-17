package com.nutriconsultas.subscription.lifecycle;

import java.io.IOException;

import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.nutriconsultas.platform.PlatformAdminService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SubscriptionAccessInterceptor implements HandlerInterceptor {

	private final SubscriptionAccessService subscriptionAccessService;

	private final PlatformAdminService platformAdminService;

	public SubscriptionAccessInterceptor(final SubscriptionAccessService subscriptionAccessService,
			final PlatformAdminService platformAdminService) {
		this.subscriptionAccessService = subscriptionAccessService;
		this.platformAdminService = platformAdminService;
	}

	@Override
	public boolean preHandle(@NonNull final HttpServletRequest request, @NonNull final HttpServletResponse response,
			@NonNull final Object handler) throws IOException {
		if (!request.getRequestURI().startsWith("/admin")) {
			return true;
		}
		if (isAllowedWithoutSubscription(request.getRequestURI())) {
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
		response.sendRedirect(request.getContextPath() + "/admin/subscription/billing");
		return false;
	}

	private static boolean isAllowedWithoutSubscription(final String uri) {
		return uri.startsWith("/admin/subscription/billing") || uri.startsWith("/admin/platform");
	}

}
