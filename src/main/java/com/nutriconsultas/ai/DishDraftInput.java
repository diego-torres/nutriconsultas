package com.nutriconsultas.ai;

import java.util.List;

import org.springframework.lang.Nullable;

/**
 * Input for {@code create_dish_draft}.
 */
public record DishDraftInput(String name, @Nullable String description, @Nullable List<String> preparationSteps,
		@Nullable String ingestasSugeridas, List<RecipeIngredientInput> ingredients, @Nullable Integer portions,
		@Nullable NutrientSummary nutrientsPerPortion, @Nullable List<String> assumptions,
		@Nullable List<String> warnings) {
}
