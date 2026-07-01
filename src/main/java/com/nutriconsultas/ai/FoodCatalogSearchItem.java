package com.nutriconsultas.ai;

import org.springframework.lang.Nullable;

/**
 * Single row returned by {@code search_food_catalog}.
 */
public record FoodCatalogSearchItem(long alimentoId, String nombreAlimento, @Nullable String clasificacion,
		@Nullable String unidad, @Nullable Double cantSugerida, @Nullable Integer energiaKcalPorPorcion) {
}
