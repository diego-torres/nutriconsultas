package com.nutriconsultas.paciente;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.nutriconsultas.reports.ReportEntitlementModelHelper;
import com.nutriconsultas.subscription.SubscriptionEntitlementService;

/**
 * Adds report entitlement flags to patient profile views without growing
 * PacienteController.
 */
@ControllerAdvice(assignableTypes = PacienteController.class)
public class PacienteProfileReportModelAdvice {

	private final SubscriptionEntitlementService subscriptionEntitlementService;

	public PacienteProfileReportModelAdvice(final SubscriptionEntitlementService subscriptionEntitlementService) {
		this.subscriptionEntitlementService = subscriptionEntitlementService;
	}

	@ModelAttribute
	public void addReportEntitlementFlags(@AuthenticationPrincipal final OidcUser principal, final Model model) {
		if (principal == null) {
			return;
		}
		ReportEntitlementModelHelper.addPatientReportFlags(model, principal.getSubject(),
				subscriptionEntitlementService);
	}

}
