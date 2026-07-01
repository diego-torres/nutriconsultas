package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateDishDraftToolServiceTest {

	private static final String NUTRITIONIST_ID = "auth0|nutritionist-a";

	private static final long THREAD_ID = 42L;

	@InjectMocks
	private CreateDishDraftToolServiceImpl service;

	@Mock
	private AiDraftLifecycleService draftLifecycleService;

	@Mock
	private CalculateRecipeNutrientsToolService recipeNutrientsToolService;

	@Test
	void createDraftPersistsDishDraftWithComputedNutrients() {
		final RecipeNutrientsData nutrients = new RecipeNutrientsData(1, List.of(),
				new NutrientSummary(250, 20.0, 8.0, 30.0, 4.0, 300.0, 500.0),
				new NutrientSummary(250, 20.0, 8.0, 30.0, 4.0, 300.0, 500.0));
		when(recipeNutrientsToolService.calculate(eq(NUTRITIONIST_ID), any(), eq(2), eq(null)))
			.thenReturn(AiToolResult.success(nutrients));
		final AiGeneratedDraft saved = new AiGeneratedDraft();
		saved.setId(99L);
		saved.setStatus(AiDraftStatus.DRAFT);
		when(draftLifecycleService.createDraft(eq(THREAD_ID), eq(NUTRITIONIST_ID), eq(AiDraftType.DISH), any()))
			.thenReturn(saved);

		final DishDraftInput input = new DishDraftInput("Tacos de pollo", "Receta ligera", List.of("Cocinar el pollo"),
				"Comida", List.of(new RecipeIngredientInput(1L, "1", null, null)), 2, null, List.of("Porción estándar"),
				List.of("Revisar sodio"));
		final AiToolResult<AiDraftCreationData> result = service.createDraft(NUTRITIONIST_ID, THREAD_ID, input);

		assertThat(result.success()).isTrue();
		assertThat(result.data().draftId()).isEqualTo(99L);
		assertThat(result.data().draftType()).isEqualTo(AiDraftType.DISH);
		assertThat(result.data().status()).isEqualTo(AiDraftStatus.DRAFT);
		assertThat(result.data().summary()).contains("Tacos de pollo");
		verify(draftLifecycleService).createDraft(eq(THREAD_ID), eq(NUTRITIONIST_ID), eq(AiDraftType.DISH), any());
	}

	@Test
	void createDraftRejectsInvalidName() {
		final DishDraftInput input = new DishDraftInput("a", null, null, null,
				List.of(new RecipeIngredientInput(1L, "1", null, null)), 1, null, null, null);

		final AiToolResult<AiDraftCreationData> result = service.createDraft(NUTRITIONIST_ID, THREAD_ID, input);

		assertThat(result.success()).isFalse();
		assertThat(result.errorCode()).isEqualTo(AiToolErrorCode.VALIDATION);
		verify(draftLifecycleService, never()).createDraft(any(), any(), any(), any());
	}

	@Test
	void createDraftPropagatesNutrientCalculationErrors() {
		when(recipeNutrientsToolService.calculate(eq(NUTRITIONIST_ID), any(), any(), any()))
			.thenReturn(AiToolResult.error(AiToolErrorCode.NOT_FOUND, "No se encontró el alimento solicitado."));

		final DishDraftInput input = new DishDraftInput("Ensalada", null, null, null,
				List.of(new RecipeIngredientInput(99L, "1", null, null)), 1, null, null, null);
		final AiToolResult<AiDraftCreationData> result = service.createDraft(NUTRITIONIST_ID, THREAD_ID, input);

		assertThat(result.success()).isFalse();
		assertThat(result.errorCode()).isEqualTo(AiToolErrorCode.NOT_FOUND);
		verify(draftLifecycleService, never()).createDraft(any(), any(), any(), any());
	}

	@Test
	void createDraftMapsUnknownThreadToNotFound() {
		when(recipeNutrientsToolService.calculate(eq(NUTRITIONIST_ID), any(), any(), any())).thenReturn(AiToolResult
			.success(new RecipeNutrientsData(1, List.of(), new NutrientSummary(100, 10.0, 5.0, 12.0, 2.0, 100.0, 200.0),
					new NutrientSummary(100, 10.0, 5.0, 12.0, 2.0, 100.0, 200.0))));
		when(draftLifecycleService.createDraft(eq(THREAD_ID), eq(NUTRITIONIST_ID), eq(AiDraftType.DISH), any()))
			.thenThrow(new AiDraftLifecycleException("Conversación no encontrada."));

		final DishDraftInput input = new DishDraftInput("Sopa", null, null, null,
				List.of(new RecipeIngredientInput(1L, "1", null, null)), 1, null, null, null);
		final AiToolResult<AiDraftCreationData> result = service.createDraft(NUTRITIONIST_ID, THREAD_ID, input);

		assertThat(result.success()).isFalse();
		assertThat(result.errorCode()).isEqualTo(AiToolErrorCode.NOT_FOUND);
	}

}
