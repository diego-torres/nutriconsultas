package com.nutriconsultas.ai;

import org.springframework.lang.Nullable;

/**
 * Ingredient line for {@code calculate_recipe_nutrients}.
 */
public record RecipeIngredientInput(long alimentoId, String cantidad, @Nullable Integer pesoNetoG,
		@Nullable String unidad) {
}
