package com.nutriconsultas.ai;

import java.util.List;

import org.springframework.lang.Nullable;

/**
 * {@code data} payload for {@code validate_plan_constraints}.
 */
public record PlanConstraintValidationData(boolean valid, @Nullable NutrientSummary computedNutrients,
		List<PlanConstraintWarning> warnings, boolean patientContextApplied) {
}
