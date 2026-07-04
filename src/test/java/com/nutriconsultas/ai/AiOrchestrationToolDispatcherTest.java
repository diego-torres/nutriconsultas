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

	@Mock
	private AiDraftToolSchemaValidator draftToolSchemaValidator;

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

	@Test
	void dispatchDishDraftRejectsSchemaViolationBeforeService() {
		when(draftToolSchemaValidator.validateDishDraftArguments(org.mockito.ArgumentMatchers.anyString())).thenReturn(
				java.util.Optional.of("Argumentos de herramienta no válidos: falta el campo obligatorio (name)."));

		final String json = dispatcher.dispatch(context(), CreateDishDraftToolService.TOOL_NAME,
				"{\"ingredients\":[{\"alimentoId\":1,\"cantidad\":\"1\"}]}");

		assertThat(json).contains("\"success\":false");
		assertThat(json).contains("obligatorio");
		org.mockito.Mockito.verify(createDishDraftToolService, org.mockito.Mockito.never())
			.createDraft(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyLong(),
					org.mockito.ArgumentMatchers.any());
	}

	@Test
	void dispatchDishDraftUsesRealSchemaValidatorWhenConfigured() {
		final AiOrchestrationToolDispatcher realSchemaDispatcher = new AiOrchestrationToolDispatcher(
				searchFoodCatalogToolService, getFoodNutrientsToolService, searchDishCatalogToolService,
				calculateRecipeNutrientsToolService, validatePlanConstraintsToolService, createDishDraftToolService,
				createMenuDraftToolService, createDietPlanDraftToolService, new AiDraftToolSchemaValidator());
		when(createDishDraftToolService.createDraft(org.mockito.ArgumentMatchers.eq(NUTRITIONIST_ID),
				org.mockito.ArgumentMatchers.eq(THREAD_ID), org.mockito.ArgumentMatchers.any()))
			.thenReturn(AiToolResult
				.success(new AiDraftCreationData(1L, AiDraftType.DISH, AiDraftStatus.DRAFT, "Borrador IA — Tacos")));

		final String json = realSchemaDispatcher.dispatch(context(), CreateDishDraftToolService.TOOL_NAME, """
				{
				  "name": "Tacos",
				  "ingredients": [{ "alimentoId": 1, "cantidad": "1" }]
				}
				""");

		assertThat(json).contains("\"success\":true");
	}

	private static AiOrchestrationContext context() {
		return new AiOrchestrationContext(NUTRITIONIST_ID, THREAD_ID, null, null, null);
	}

}
