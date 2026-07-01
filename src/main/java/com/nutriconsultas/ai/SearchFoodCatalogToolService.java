package com.nutriconsultas.ai;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * AI tool {@code search_food_catalog} — read-only global food catalog search.
 */
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface SearchFoodCatalogToolService {

	String TOOL_NAME = "search_food_catalog";

	AiToolResult<FoodCatalogSearchData> search(@NonNull String nutritionistId, @NonNull String query,
			@Nullable String clasificacion, @Nullable Integer limit);

}
