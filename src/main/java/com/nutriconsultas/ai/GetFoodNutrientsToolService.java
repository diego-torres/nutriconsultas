package com.nutriconsultas.ai;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * AI tool {@code get_food_nutrients} — nutrient lookup for a catalog food portion.
 */
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface GetFoodNutrientsToolService {

	String TOOL_NAME = "get_food_nutrients";

	AiToolResult<FoodNutrientsData> getNutrients(@NonNull String nutritionistId, long alimentoId,
			@NonNull String cantidad, @Nullable Integer pesoNetoG, @Nullable Integer portions, @Nullable String unidad);

}
