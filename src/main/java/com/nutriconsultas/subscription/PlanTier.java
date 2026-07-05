package com.nutriconsultas.subscription;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Canonical subscription plan tiers aligned with marketing pricing on
 * {@code eterna/index.html}.
 */
public enum PlanTier {

	BASICO("nutriologo-basico", 10, 1,
			EnumSet.of(Entitlement.PATIENT_MANAGEMENT, Entitlement.DIET_PLANS, Entitlement.CALENDAR,
					Entitlement.REPORTS_BASIC)),

	PROFESIONAL("nutriologo-profesional", 50, 1,
			EnumSet.of(Entitlement.PATIENT_MANAGEMENT, Entitlement.DIET_PLANS, Entitlement.CALENDAR,
					Entitlement.REPORTS_BASIC, Entitlement.REPORTS_ADVANCED, Entitlement.REPORTS_FULL,
					Entitlement.PDF_EXPORT, Entitlement.REPORTS_BRANDED)),

	PLUS("nutriologo-plus", null, 1,
			EnumSet.of(Entitlement.PATIENT_MANAGEMENT, Entitlement.DIET_PLANS, Entitlement.CALENDAR,
					Entitlement.REPORTS_BASIC, Entitlement.REPORTS_ADVANCED, Entitlement.REPORTS_FULL,
					Entitlement.PDF_EXPORT, Entitlement.REPORTS_BRANDED, Entitlement.PRIORITY_SUPPORT,
					Entitlement.AI_ASSISTANT)),

	CONSULTORIO("director-consultorio", null, 20,
			EnumSet.of(Entitlement.PATIENT_MANAGEMENT, Entitlement.DIET_PLANS, Entitlement.CALENDAR,
					Entitlement.REPORTS_BASIC, Entitlement.REPORTS_ADVANCED, Entitlement.REPORTS_FULL,
					Entitlement.PDF_EXPORT, Entitlement.REPORTS_BRANDED, Entitlement.PRIORITY_SUPPORT,
					Entitlement.USER_ADMINISTRATION, Entitlement.AI_ASSISTANT));

	private final String roleSlug;

	private final Integer maxPatients;

	private final int maxNutritionists;

	private final Set<Entitlement> entitlements;

	PlanTier(final String roleSlug, final Integer maxPatients, final int maxNutritionists,
			final Set<Entitlement> entitlements) {
		this.roleSlug = roleSlug;
		this.maxPatients = maxPatients;
		this.maxNutritionists = maxNutritionists;
		this.entitlements = Collections.unmodifiableSet(entitlements);
	}

	public String getRoleSlug() {
		return roleSlug;
	}

	/**
	 * Maximum patients allowed for the plan, or {@code null} for unlimited.
	 */
	public Integer getMaxPatients() {
		return maxPatients;
	}

	public int getMaxNutritionists() {
		return maxNutritionists;
	}

	public Set<Entitlement> getEntitlements() {
		return entitlements;
	}

	public boolean hasEntitlement(final Entitlement entitlement) {
		return entitlements.contains(entitlement);
	}

	public String getDisplayName() {
		return switch (this) {
			case BASICO -> "Básico";
			case PROFESIONAL -> "Profesional";
			case PLUS -> "Plus";
			case CONSULTORIO -> "Consultorio";
		};
	}

	public static PlanTier fromRoleSlug(final String roleSlug) {
		for (final PlanTier tier : values()) {
			if (tier.roleSlug.equals(roleSlug)) {
				return tier;
			}
		}
		throw new IllegalArgumentException("Unknown plan role slug: " + roleSlug);
	}

}
