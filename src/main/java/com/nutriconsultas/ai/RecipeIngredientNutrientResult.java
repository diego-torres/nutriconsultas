package com.nutriconsultas.ai;

import java.util.List;

/**
 * Per-ingredient nutrient result for {@code calculate_recipe_nutrients}.
 */
public record RecipeIngredientNutrientResult(long alimentoId, NutrientSummary nutrients, List<String> warnings) {
}
