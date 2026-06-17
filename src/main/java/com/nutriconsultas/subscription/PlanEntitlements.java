package com.nutriconsultas.subscription;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable plan limits and feature flags aligned with marketing pricing on
 * {@code eterna/index.html}. Canonical matrix:
 * {@code docs/subscription/SUBSCRIPTION-ENFORCEMENT-PLAN.md}.
 */
public final class PlanEntitlements {

	public static final PlanEntitlements BASICO = new PlanEntitlements(PlanTier.BASICO, "nutriologo-basico", 10, 1,
			EnumSet.of(Entitlement.PATIENT_MANAGEMENT, Entitlement.CREATE_PATIENT, Entitlement.DIET_PLANS,
					Entitlement.CALENDAR, Entitlement.REPORTS_BASIC));

	public static final PlanEntitlements PROFESIONAL = new PlanEntitlements(PlanTier.PROFESIONAL,
			"nutriologo-profesional", 50, 1,
			EnumSet.of(Entitlement.PATIENT_MANAGEMENT, Entitlement.CREATE_PATIENT, Entitlement.DIET_PLANS,
					Entitlement.CALENDAR, Entitlement.REPORTS_BASIC, Entitlement.REPORTS_ADVANCED,
					Entitlement.REPORTS_FULL, Entitlement.PDF_EXPORT, Entitlement.REPORTS_BRANDED));

	public static final PlanEntitlements PLUS = new PlanEntitlements(PlanTier.PLUS, "nutriologo-plus", null, 1,
			EnumSet.of(Entitlement.PATIENT_MANAGEMENT, Entitlement.CREATE_PATIENT, Entitlement.DIET_PLANS,
					Entitlement.CALENDAR, Entitlement.REPORTS_BASIC, Entitlement.REPORTS_ADVANCED,
					Entitlement.REPORTS_FULL, Entitlement.PDF_EXPORT, Entitlement.REPORTS_BRANDED,
					Entitlement.PRIORITY_SUPPORT));

	public static final PlanEntitlements CONSULTORIO = new PlanEntitlements(PlanTier.CONSULTORIO,
			"director-consultorio", null, 20,
			EnumSet.of(Entitlement.PATIENT_MANAGEMENT, Entitlement.CREATE_PATIENT, Entitlement.DIET_PLANS,
					Entitlement.CALENDAR, Entitlement.REPORTS_BASIC, Entitlement.REPORTS_ADVANCED,
					Entitlement.REPORTS_FULL, Entitlement.PDF_EXPORT, Entitlement.REPORTS_BRANDED,
					Entitlement.PRIORITY_SUPPORT, Entitlement.USER_ADMINISTRATION));

	private final PlanTier planTier;

	private final String roleSlug;

	private final Integer maxPatients;

	private final int maxNutritionists;

	private final Set<Entitlement> entitlements;

	private PlanEntitlements(final PlanTier planTier, final String roleSlug, final Integer maxPatients,
			final int maxNutritionists, final Set<Entitlement> entitlements) {
		this.planTier = planTier;
		this.roleSlug = roleSlug;
		this.maxPatients = maxPatients;
		this.maxNutritionists = maxNutritionists;
		this.entitlements = Collections.unmodifiableSet(EnumSet.copyOf(entitlements));
	}

	public PlanTier getPlanTier() {
		return planTier;
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

	public static PlanEntitlements forTier(final PlanTier planTier) {
		Objects.requireNonNull(planTier, "planTier");
		return switch (planTier) {
			case BASICO -> BASICO;
			case PROFESIONAL -> PROFESIONAL;
			case PLUS -> PLUS;
			case CONSULTORIO -> CONSULTORIO;
		};
	}

	public static PlanEntitlements fromRoleSlug(final String roleSlug) {
		for (final PlanEntitlements config : new PlanEntitlements[] { BASICO, PROFESIONAL, PLUS, CONSULTORIO }) {
			if (config.roleSlug.equals(roleSlug)) {
				return config;
			}
		}
		throw new IllegalArgumentException("Unknown plan role slug: " + roleSlug);
	}

}
