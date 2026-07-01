package com.nutriconsultas.ai;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * AI tool {@code validate_plan_constraints} — read-only draft constraint validation.
 */
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface ValidatePlanConstraintsToolService {

	String TOOL_NAME = "validate_plan_constraints";

	AiToolResult<PlanConstraintValidationData> validate(@NonNull String nutritionistId,
			@NonNull ValidatePlanConstraintsRequest request, @Nullable AiPatientPromptContext patientContext);

}
