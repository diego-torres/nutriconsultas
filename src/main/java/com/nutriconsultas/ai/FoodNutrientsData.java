package com.nutriconsultas.ai;

import org.springframework.lang.Nullable;

/**
 * {@code data} payload for {@code get_food_nutrients}.
 */
public record FoodNutrientsData(long alimentoId, String nombreAlimento, String cantidad, @Nullable Integer pesoNetoG,
		NutrientSummary nutrientsPerCalculation, NutrientSummary nutrientsTotal) {
}
