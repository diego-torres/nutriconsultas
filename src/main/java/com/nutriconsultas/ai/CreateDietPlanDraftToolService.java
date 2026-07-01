package com.nutriconsultas.ai;

import org.springframework.lang.NonNull;

/**
 * AI tool {@code create_diet_plan_draft} — persist a multi-day diet plan draft for
 * nutritionist review.
 */
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface CreateDietPlanDraftToolService {

	String TOOL_NAME = "create_diet_plan_draft";

	AiToolResult<AiDraftCreationData> createDraft(@NonNull String nutritionistId, long threadId,
			@NonNull DietPlanDraftInput input);

}
