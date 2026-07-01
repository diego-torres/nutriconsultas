package com.nutriconsultas.ai;

/**
 * Structured warning from {@code validate_plan_constraints}.
 */
public record PlanConstraintWarning(PlanConstraintWarningCode code, String message,
		PlanConstraintWarningSeverity severity) {
}
