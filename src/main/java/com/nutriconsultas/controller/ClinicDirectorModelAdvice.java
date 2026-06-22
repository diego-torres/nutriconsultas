package com.nutriconsultas.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.nutriconsultas.subscription.Entitlement;
import com.nutriconsultas.subscription.SubscriptionEntitlementService;

@Component
@ControllerAdvice
public class ClinicDirectorModelAdvice {

	private final SubscriptionEntitlementService subscriptionEntitlementService;

	public ClinicDirectorModelAdvice(final SubscriptionEntitlementService subscriptionEntitlementService) {
		this.subscriptionEntitlementService = subscriptionEntitlementService;
	}

	@ModelAttribute
	public void addClinicDirectorFlag(@AuthenticationPrincipal final OidcUser principal, final Model model) {
		final boolean clinicDirector = principal != null && subscriptionEntitlementService
			.hasEntitlement(principal.getSubject(), Entitlement.USER_ADMINISTRATION);
		model.addAttribute("clinicDirector", clinicDirector);
	}

}
