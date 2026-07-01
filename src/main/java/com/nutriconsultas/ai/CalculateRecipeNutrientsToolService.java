package com.nutriconsultas.ai;

import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * AI tool {@code calculate_recipe_nutrients} — aggregate nutrients for a recipe
 * ingredient list.
 */
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface CalculateRecipeNutrientsToolService {

	String TOOL_NAME = "calculate_recipe_nutrients";

	AiToolResult<RecipeNutrientsData> calculate(@NonNull String nutritionistId,
			@NonNull List<RecipeIngredientInput> ingredients, @Nullable Integer portions, @Nullable String label);

}
