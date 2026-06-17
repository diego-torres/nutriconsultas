package com.nutriconsultas.subscription.lifecycle;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.nutriconsultas.platform.PlatformAdminService;

@Component
@ControllerAdvice
public class SubscriptionBannerAdvice {

	private final SubscriptionAccessService subscriptionAccessService;

	private final PlatformAdminService platformAdminService;

	public SubscriptionBannerAdvice(final SubscriptionAccessService subscriptionAccessService,
			final PlatformAdminService platformAdminService) {
		this.subscriptionAccessService = subscriptionAccessService;
		this.platformAdminService = platformAdminService;
	}

	@ModelAttribute
	public void addSubscriptionBanner(@AuthenticationPrincipal final OidcUser principal, final Model model) {
		if (principal == null || platformAdminService.isPlatformAdmin(principal)) {
			return;
		}
		subscriptionAccessService.resolveBanner(principal.getSubject()).ifPresent(banner -> {
			model.addAttribute("subscriptionBanner", banner);
		});
	}

}
