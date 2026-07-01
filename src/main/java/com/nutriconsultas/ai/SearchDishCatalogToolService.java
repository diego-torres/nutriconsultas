package com.nutriconsultas.ai;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * AI tool {@code search_dish_catalog} — authorized platillo catalog search.
 */
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface SearchDishCatalogToolService {

	String TOOL_NAME = "search_dish_catalog";

	AiToolResult<DishCatalogSearchData> search(@NonNull String nutritionistId, @NonNull String query,
			@Nullable String ingestasSugeridas, @Nullable Integer limit);

}
