package com.nutriconsultas.admin;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.nutriconsultas.controller.AbstractAuthorizedController;
import com.nutriconsultas.subscription.lifecycle.SubscriptionAccessService;

@Controller
@RequestMapping("/admin/subscription")
public class SubscriptionBillingController extends AbstractAuthorizedController {

	private final SubscriptionAccessService subscriptionAccessService;

	public SubscriptionBillingController(final SubscriptionAccessService subscriptionAccessService) {
		this.subscriptionAccessService = subscriptionAccessService;
	}

	@GetMapping("/billing")
	public String billing(@AuthenticationPrincipal final OidcUser principal, final Model model) {
		subscriptionAccessService.findSubscriptionForUser(principal.getSubject()).ifPresent(subscription -> {
			model.addAttribute("subscriptionStatus", subscription.getStatus());
			model.addAttribute("periodEnd", subscription.getPeriodEnd());
		});
		model.addAttribute("activeMenu", "home");
		return "sbadmin/subscription/billing";
	}

}
