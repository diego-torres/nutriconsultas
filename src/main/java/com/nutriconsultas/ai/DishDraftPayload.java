package com.nutriconsultas.ai;

import java.util.List;

import org.springframework.lang.Nullable;

/**
 * JSON payload persisted for a dish draft ({@code create_dish_draft}).
 */
public record DishDraftPayload(String name, @Nullable String description, @Nullable List<String> preparationSteps,
		@Nullable String ingestasSugeridas, List<RecipeIngredientInput> ingredients, int portions,
		NutrientSummary nutrientsPerPortion, @Nullable List<String> assumptions, @Nullable List<String> warnings,
		String label) {
}
