package com.nutriconsultas.contact;

import java.util.Optional;

import com.nutriconsultas.subscription.PlanTier;

/**
 * Resolves marketing plan slugs for the public contact funnel.
 */
public final class ContactPlanInterest {

	private ContactPlanInterest() {
	}

	public static Optional<PlanTier> resolveFromParam(final String planParam) {
		if (planParam == null || planParam.isBlank()) {
			return Optional.empty();
		}
		try {
			return Optional.of(PlanTier.fromRoleSlug(planParam.trim()));
		}
		catch (IllegalArgumentException ex) {
			return Optional.empty();
		}
	}

	public static String normalizeForStorage(final String planRoleSlug) {
		return resolveFromParam(planRoleSlug).map(PlanTier::getRoleSlug).orElse(null);
	}

	public static String defaultSubjectForPlan(final PlanTier planTier) {
		return "Solicitud de acceso — Plan " + planTier.getDisplayName();
	}

}
