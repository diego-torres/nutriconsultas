package com.nutriconsultas.ai;

import org.springframework.lang.Nullable;

/**
 * Single row returned by {@code search_dish_catalog}.
 */
public record DishCatalogSearchItem(long platilloId, String name, @Nullable String ingestasSugeridas,
		@Nullable Integer energiaKcal, @Nullable Double proteinaG, boolean ownedByNutritionist, boolean systemCatalog) {
}
