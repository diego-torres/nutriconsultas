package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiOrchestrationToolDispatcherTest {

	private static final String NUTRITIONIST_ID = "auth0|nutritionist-a";

	private static final long THREAD_ID = 7L;

	@InjectMocks
	private AiOrchestrationToolDispatcher dispatcher;

	@Mock
	private SearchFoodCatalogToolService searchFoodCatalogToolService;

	@Mock
	private GetFoodNutrientsToolService getFoodNutrientsToolService;

	@Mock
	private SearchDishCatalogToolService searchDishCatalogToolService;

	@Mock
	private CalculateRecipeNutrientsToolService calculateRecipeNutrientsToolService;

	@Mock
	private ValidatePlanConstraintsToolService validatePlanConstraintsToolService;

	@Mock
	private CreateDishDraftToolService createDishDraftToolService;

	@Mock
	private CreateMenuDraftToolService createMenuDraftToolService;

	@Mock
	private CreateDietPlanDraftToolService createDietPlanDraftToolService;

	@Test
	void dispatchSearchFoodCatalog() {
		final FoodCatalogSearchData data = new FoodCatalogSearchData(java.util.List.of(), 0, false);
		when(searchFoodCatalogToolService.search(eq(NUTRITIONIST_ID), eq("avena"), eq(null), eq(5)))
			.thenReturn(AiToolResult.success(data));

		final String json = dispatcher.dispatch(context(), SearchFoodCatalogToolService.TOOL_NAME,
				"{\"query\":\"avena\",\"limit\":5}");

		assertThat(json).contains("\"success\":true");
		verify(searchFoodCatalogToolService).search(NUTRITIONIST_ID, "avena", null, 5);
	}

	@Test
	void dispatchUnknownToolReturnsValidationError() {
		final String json = dispatcher.dispatch(context(), "unknown_tool", "{}");

		assertThat(json).contains("\"success\":false");
		assertThat(json).contains("Herramienta no reconocida");
	}

	private static AiOrchestrationContext context() {
		return new AiOrchestrationContext(NUTRITIONIST_ID, THREAD_ID, null);
	}

}
