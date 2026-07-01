package com.nutriconsultas.ai;

import java.util.List;

import org.springframework.lang.Nullable;

/**
 * Dish/recipe payload for plan validation.
 */
public record DishPlanInput(List<RecipeIngredientInput> ingredients, @Nullable Integer portions) {
}
