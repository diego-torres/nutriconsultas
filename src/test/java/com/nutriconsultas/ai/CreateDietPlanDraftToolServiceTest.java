package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateDietPlanDraftToolServiceTest {

	private static final String NUTRITIONIST_ID = "auth0|nutritionist-a";

	private static final long THREAD_ID = 42L;

	@InjectMocks
	private CreateDietPlanDraftToolServiceImpl service;

	@Mock
	private AiDraftLifecycleService draftLifecycleService;

	@Mock
	private AiIngestaNutrientCalculator ingestaNutrientCalculator;

	@Test
	void createDraftPersistsDietPlanDraftWithComputedNutrients() {
		final NutrientSummary dayNutrients = new NutrientSummary(1800, 90.0, 60.0, 200.0, 25.0, 2000.0, 3500.0);
		when(ingestaNutrientCalculator.computeIngestas(eq(NUTRITIONIST_ID), any())).thenReturn(AiToolResult
			.success(new AiIngestaNutrientCalculator.IngestaNutrientComputation(dayNutrients, List.of(), Set.of(1L))));
		final AiGeneratedDraft saved = new AiGeneratedDraft();
		saved.setId(77L);
		saved.setStatus(AiDraftStatus.DRAFT);
		when(draftLifecycleService.createDraft(eq(THREAD_ID), eq(NUTRITIONIST_ID), eq(AiDraftType.DIET_PLAN), any()))
			.thenReturn(saved);

		final DietPlanDraftInput input = new DietPlanDraftInput("Plan semanal", 2, 1800.0,
				List.of(day(1, "Lunes"), day(2, "Martes")), "Cumple objetivos", List.of("Sin restricciones"),
				List.of("Revisar fibra"));
		final AiToolResult<AiDraftCreationData> result = service.createDraft(NUTRITIONIST_ID, THREAD_ID, input);

		assertThat(result.success()).isTrue();
		assertThat(result.data().draftId()).isEqualTo(77L);
		assertThat(result.data().draftType()).isEqualTo(AiDraftType.DIET_PLAN);
		assertThat(result.data().status()).isEqualTo(AiDraftStatus.DRAFT);
		assertThat(result.data().summary()).contains("Plan semanal");
		verify(draftLifecycleService).createDraft(eq(THREAD_ID), eq(NUTRITIONIST_ID), eq(AiDraftType.DIET_PLAN), any());
		verify(ingestaNutrientCalculator, times(2)).computeIngestas(eq(NUTRITIONIST_ID), any());
	}

	@Test
	void createDraftRejectsEmptyDays() {
		final DietPlanDraftInput input = new DietPlanDraftInput(null, null, null, List.of(), null, null, null);

		final AiToolResult<AiDraftCreationData> result = service.createDraft(NUTRITIONIST_ID, THREAD_ID, input);

		assertThat(result.success()).isFalse();
		assertThat(result.errorCode()).isEqualTo(AiToolErrorCode.VALIDATION);
		verify(ingestaNutrientCalculator, never()).computeIngestas(any(), any());
		verify(draftLifecycleService, never()).createDraft(any(), any(), any(), any());
	}

	@Test
	void createDraftRejectsMismatchedDayCount() {
		final DietPlanDraftInput input = new DietPlanDraftInput(null, 3, null, List.of(day(1, null)), null, null, null);

		final AiToolResult<AiDraftCreationData> result = service.createDraft(NUTRITIONIST_ID, THREAD_ID, input);

		assertThat(result.success()).isFalse();
		assertThat(result.errorCode()).isEqualTo(AiToolErrorCode.VALIDATION);
		verify(ingestaNutrientCalculator, never()).computeIngestas(any(), any());
	}

	@Test
	void createDraftPropagatesNutrientComputationErrors() {
		when(ingestaNutrientCalculator.computeIngestas(eq(NUTRITIONIST_ID), any()))
			.thenReturn(AiToolResult.error(AiToolErrorCode.NOT_FOUND, "No se encontró el platillo solicitado."));

		final DietPlanDraftInput input = new DietPlanDraftInput(null, null, null,
				List.of(new DietPlanDayInput(1, null,
						List.of(new IngestaSlotInput("Comida", 1,
								List.of(new IngestaSlotItemInput("PLATILLO", 99L, null, 1, null)))))),
				null, null, null);
		final AiToolResult<AiDraftCreationData> result = service.createDraft(NUTRITIONIST_ID, THREAD_ID, input);

		assertThat(result.success()).isFalse();
		assertThat(result.errorCode()).isEqualTo(AiToolErrorCode.NOT_FOUND);
		verify(draftLifecycleService, never()).createDraft(any(), any(), any(), any());
	}

	@Test
	void createDraftMapsUnknownThreadToNotFound() {
		when(ingestaNutrientCalculator.computeIngestas(eq(NUTRITIONIST_ID), any()))
			.thenReturn(AiToolResult.success(new AiIngestaNutrientCalculator.IngestaNutrientComputation(
					new NutrientSummary(500, 20.0, 10.0, 60.0, 5.0, 400.0, 800.0), List.of(), Set.of(1L))));
		when(draftLifecycleService.createDraft(eq(THREAD_ID), eq(NUTRITIONIST_ID), eq(AiDraftType.DIET_PLAN), any()))
			.thenThrow(new AiDraftLifecycleException("Conversación no encontrada."));

		final DietPlanDraftInput input = new DietPlanDraftInput(null, null, null, List.of(day(1, null)), null, null,
				null);
		final AiToolResult<AiDraftCreationData> result = service.createDraft(NUTRITIONIST_ID, THREAD_ID, input);

		assertThat(result.success()).isFalse();
		assertThat(result.errorCode()).isEqualTo(AiToolErrorCode.NOT_FOUND);
	}

	private static DietPlanDayInput day(final int dayIndex, final String label) {
		return new DietPlanDayInput(dayIndex, label, List
			.of(new IngestaSlotInput("Desayuno", 1, List.of(new IngestaSlotItemInput("ALIMENTO", null, 1L, 1, null)))));
	}

}
