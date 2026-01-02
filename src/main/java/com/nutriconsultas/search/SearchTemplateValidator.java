package com.nutriconsultas.search;

import java.util.ArrayList;
import java.util.Map;

import com.nutriconsultas.validation.template.BaseTemplateValidator;

/**
 * Validator for search templates. Provides mock variables for search results page.
 */
public class SearchTemplateValidator extends BaseTemplateValidator {

	@Override
	public String getTemplatePathPattern() {
		return "sbadmin/search/*";
	}

	@Override
	public Map<String, Object> createMockModelVariables() {
		final Map<String, Object> variables = super.createMockModelVariables();

		// Mock search query
		variables.put("query", "");
		variables.put("category", "pacientes");
		variables.put("page", 1);

		// Mock paginated search results
		final PaginatedSearchResults emptyPaginated = new PaginatedSearchResults(new ArrayList<>(), 0, 1, 20, 0);
		final SearchResponse searchResponse = new SearchResponse("", emptyPaginated, emptyPaginated, emptyPaginated,
				emptyPaginated, emptyPaginated, 0);
		variables.put("searchResponse", searchResponse);

		return variables;
	}

}
