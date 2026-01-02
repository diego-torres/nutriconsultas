package com.nutriconsultas.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.ui.Model;

import lombok.extern.slf4j.Slf4j;

@ExtendWith(MockitoExtension.class)
@Slf4j
@ActiveProfiles("test")
@SuppressWarnings("null")
public class SearchControllerTest {

	@InjectMocks
	private SearchController controller;

	@Mock
	private SearchService searchService;

	@Mock
	private Model model;

	@Mock
	private OidcUser principal;

	private static final String TEST_USER_ID = "test-user-id-123";

	@BeforeEach
	public void setup() {
		log.info("setting up SearchController test");
		lenient().when(principal.getSubject()).thenReturn(TEST_USER_ID);
		log.info("finished setting up SearchController test");
	}

	@Test
	public void testSearchWithQuery() {
		log.info("starting testSearchWithQuery");
		// Arrange
		final String query = "test";
		final PaginatedSearchResults emptyPaginated = new PaginatedSearchResults(java.util.Collections.emptyList(), 0,
				1, 20, 0);
		final SearchResponse searchResponse = new SearchResponse(query, emptyPaginated, emptyPaginated, emptyPaginated,
				emptyPaginated, emptyPaginated, 0);
		when(searchService.search(anyString(), anyString(), anyString(), anyInt())).thenReturn(searchResponse);

		// Act
		final String result = controller.search(query, "pacientes", 1, principal, model);

		// Assert
		assertThat(result).isEqualTo("sbadmin/search/results");
		verify(searchService).search(query, TEST_USER_ID, "pacientes", 1);
		verify(model).addAttribute("query", query);
		verify(model).addAttribute("category", "pacientes");
		verify(model).addAttribute("page", 1);
		verify(model).addAttribute("searchResponse", searchResponse);
		verify(model).addAttribute("activeMenu", "search");
		log.info("finished testSearchWithQuery");
	}

	@Test
	public void testSearchWithEmptyQuery() {
		log.info("starting testSearchWithEmptyQuery");
		// Arrange
		final String query = "";

		// Act
		final String result = controller.search(query, "pacientes", 1, principal, model);

		// Assert
		assertThat(result).isEqualTo("sbadmin/search/results");
		verify(model).addAttribute("query", "");
		verify(model).addAttribute("category", "pacientes");
		verify(model).addAttribute(eq("searchResponse"), any(SearchResponse.class));
		verify(model).addAttribute("activeMenu", "search");
		log.info("finished testSearchWithEmptyQuery");
	}

	@Test
	public void testSearchWithNullQuery() {
		log.info("starting testSearchWithNullQuery");
		// Act
		final String result = controller.search(null, "pacientes", 1, principal, model);

		// Assert
		assertThat(result).isEqualTo("sbadmin/search/results");
		verify(model).addAttribute("query", "");
		verify(model).addAttribute("category", "pacientes");
		verify(model).addAttribute(eq("searchResponse"), any(SearchResponse.class));
		verify(model).addAttribute("activeMenu", "search");
		log.info("finished testSearchWithNullQuery");
	}

	@Test
	public void testSearchWithNullPrincipal() {
		log.info("starting testSearchWithNullPrincipal");
		// Arrange
		final String query = "test";

		// Act
		final String result = controller.search(query, "pacientes", 1, null, model);

		// Assert
		assertThat(result).isEqualTo("sbadmin/search/results");
		verify(model).addAttribute("query", query);
		verify(model).addAttribute("category", "pacientes");
		verify(model).addAttribute(eq("searchResponse"), any(SearchResponse.class));
		verify(model).addAttribute("activeMenu", "search");
		log.info("finished testSearchWithNullPrincipal");
	}

	@Test
	public void testSearchTrimsQuery() {
		log.info("starting testSearchTrimsQuery");
		// Arrange
		final String query = "  test  ";
		final String trimmedQuery = "test";
		final PaginatedSearchResults emptyPaginated = new PaginatedSearchResults(java.util.Collections.emptyList(), 0,
				1, 20, 0);
		final SearchResponse searchResponse = new SearchResponse(trimmedQuery, emptyPaginated, emptyPaginated,
				emptyPaginated, emptyPaginated, emptyPaginated, 0);
		when(searchService.search(anyString(), anyString(), anyString(), anyInt())).thenReturn(searchResponse);

		// Act
		final String result = controller.search(query, "pacientes", 1, principal, model);

		// Assert
		assertThat(result).isEqualTo("sbadmin/search/results");
		verify(searchService).search(trimmedQuery, TEST_USER_ID, "pacientes", 1);
		log.info("finished testSearchTrimsQuery");
	}

}