package com.nutriconsultas.search;

import java.util.ArrayList;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.nutriconsultas.controller.AbstractAuthorizedController;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@SuppressWarnings("null")
public class SearchController extends AbstractAuthorizedController {

	@Autowired
	private SearchService searchService;

	@GetMapping("/admin/search")
	public String search(@RequestParam(required = false) final String q,
			@RequestParam(required = false, defaultValue = "pacientes") final String category,
			@RequestParam(required = false, defaultValue = "1") final int page,
			@AuthenticationPrincipal final OidcUser principal, final Model model) {
		log.info("Search request with query: {}, category: {}, page: {}", q, category, page);

		if (q == null || q.trim().isEmpty()) {
			model.addAttribute("query", "");
			model.addAttribute("category", category);
			model.addAttribute("searchResponse", createEmptySearchResponse(""));
			model.addAttribute("activeMenu", "search");
			return "sbadmin/search/results";
		}

		final String userId = getUserId(principal);
		if (userId == null) {
			log.warn("User ID not found for search");
			model.addAttribute("query", q);
			model.addAttribute("category", category);
			model.addAttribute("searchResponse", createEmptySearchResponse(q));
			model.addAttribute("activeMenu", "search");
			return "sbadmin/search/results";
		}

		final String trimmedQuery = q.trim();
		final String nonNullUserId = Objects.requireNonNull(userId, "User ID must not be null");
		final String normalizedCategory = normalizeCategory(category);
		final SearchResponse searchResponse = searchService.search(trimmedQuery, nonNullUserId, normalizedCategory, page);
		model.addAttribute("query", q);
		model.addAttribute("category", normalizedCategory);
		model.addAttribute("page", page);
		model.addAttribute("searchResponse", searchResponse);
		model.addAttribute("activeMenu", "search");

		return "sbadmin/search/results";
	}

	private SearchResponse createEmptySearchResponse(final String query) {
		final PaginatedSearchResults empty = new PaginatedSearchResults(new ArrayList<>(), 0, 1, 20, 0);
		return new SearchResponse(query, empty, empty, empty, empty, empty, 0);
	}

	private String normalizeCategory(final String category) {
		if (category == null || category.isEmpty()) {
			return "pacientes";
		}
		final String normalized = category.toLowerCase();
		if ("pacientes".equals(normalized) || "alimentos".equals(normalized) || "platillos".equals(normalized)
				|| "calendarevents".equals(normalized) || "clinicalexams".equals(normalized)) {
			return normalized;
		}
		// Handle camelCase variations
		if ("calendarevents".equals(normalized) || "calendar_events".equals(normalized)
				|| "calendar-events".equals(normalized)) {
			return "calendarevents";
		}
		if ("clinicalexams".equals(normalized) || "clinical_exams".equals(normalized)
				|| "clinical-exams".equals(normalized)) {
			return "clinicalexams";
		}
		return "pacientes";
	}

	private String getUserId(final OidcUser principal) {
		if (principal == null) {
			return null;
		}
		return principal.getSubject();
	}

}
