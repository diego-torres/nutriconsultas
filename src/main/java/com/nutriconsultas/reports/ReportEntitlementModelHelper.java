package com.nutriconsultas.reports;

import org.springframework.ui.Model;

import com.nutriconsultas.subscription.Entitlement;
import com.nutriconsultas.subscription.SubscriptionEntitlementService;

/**
 * Adds report-related subscription entitlement flags to Thymeleaf models.
 */
public final class ReportEntitlementModelHelper {

	private ReportEntitlementModelHelper() {
	}

	public static void addPatientReportFlags(final Model model, final String userId,
			final SubscriptionEntitlementService subscriptionEntitlementService) {
		model.addAttribute("canExportPdf",
				subscriptionEntitlementService.hasEntitlement(userId, Entitlement.PDF_EXPORT));
		model.addAttribute("canFullReports",
				subscriptionEntitlementService.hasEntitlement(userId, Entitlement.REPORTS_FULL));
	}

}
