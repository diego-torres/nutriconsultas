package com.nutriconsultas.ai;

import java.util.List;

/**
 * {@code data} payload for {@code search_dish_catalog}.
 */
public record DishCatalogSearchData(List<DishCatalogSearchItem> items, int totalReturned) {
}
