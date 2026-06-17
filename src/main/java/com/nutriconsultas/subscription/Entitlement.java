package com.nutriconsultas.subscription;

/**
 * Feature flags gated by {@link PlanTier}. Canonical matrix:
 * {@code docs/subscription/SUBSCRIPTION-ENFORCEMENT-PLAN.md}.
 */
public enum Entitlement {

	PATIENT_MANAGEMENT, CREATE_PATIENT, DIET_PLANS, CALENDAR, REPORTS_BASIC, REPORTS_ADVANCED, REPORTS_FULL, PDF_EXPORT,
	REPORTS_BRANDED, PRIORITY_SUPPORT, USER_ADMINISTRATION

}
