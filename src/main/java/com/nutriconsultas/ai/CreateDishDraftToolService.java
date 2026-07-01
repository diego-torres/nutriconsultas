package com.nutriconsultas.ai;

import org.springframework.lang.NonNull;

/**
 * AI tool {@code create_dish_draft} — persist a dish/recipe draft for nutritionist
 * review.
 */
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface CreateDishDraftToolService {

	String TOOL_NAME = "create_dish_draft";

	AiToolResult<AiDraftCreationData> createDraft(@NonNull String nutritionistId, long threadId,
			@NonNull DishDraftInput input);

}
