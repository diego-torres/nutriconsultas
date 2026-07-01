package com.nutriconsultas.ai;

import java.util.List;

import org.springframework.lang.Nullable;

/**
 * {@code data} payload for {@code calculate_recipe_nutrients}.
 */
public record RecipeNutrientsData(int portions, @Nullable List<RecipeIngredientNutrientResult> ingredientResults,
		NutrientSummary nutrientsPerPortion, NutrientSummary nutrientsTotal) {
}
