package com.nutriconsultas.ai;

import java.util.List;

/**
 * {@code data} payload for {@code search_food_catalog}.
 */
public record FoodCatalogSearchData(List<FoodCatalogSearchItem> items, int totalReturned, boolean truncated) {
}
