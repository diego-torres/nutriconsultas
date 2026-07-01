package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
class CreateMenuDraftToolServiceTest {

	private static final String NUTRITIONIST_ID = "auth0|nutritionist-a";

	private static final long THREAD_ID = 42L;

	@InjectMocks
	private CreateMenuDraftToolServiceImpl service;

	@Mock
	private AiDraftLifecycleService draftLifecycleService;

	@Mock
	private AiIngestaNutrientCalculator ingestaNutrientCalculator;

	@Test
	void createDraftPersistsMenuDraftWithComputedNutrients() {
		final NutrientSummary nutrients = new NutrientSummary(1800, 90.0, 60.0, 200.0, 25.0, 2000.0, 3500.0);
		when(ingestaNutrientCalculator.computeIngestas(eq(NUTRITIONIST_ID), any())).thenReturn(AiToolResult
			.success(new AiIngestaNutrientCalculator.IngestaNutrientComputation(nutrients, List.of(), Set.of(1L, 2L))));
		final AiGeneratedDraft saved = new AiGeneratedDraft();
		saved.setId(88L);
		saved.setStatus(AiDraftStatus.DRAFT);
		when(draftLifecycleService.createDraft(eq(THREAD_ID), eq(NUTRITIONIST_ID), eq(AiDraftType.MENU), any()))
			.thenReturn(saved);

		final MenuDraftInput input = new MenuDraftInput("Menú balanceado", 1800.0,
				List.of(new IngestaSlotInput("Desayuno", 1,
						List.of(new IngestaSlotItemInput("ALIMENTO", null, 1L, 1, null)))),
				"Cumple objetivo calórico", List.of("Paciente sin restricciones"), List.of("Revisar sodio"));
		final AiToolResult<AiDraftCreationData> result = service.createDraft(NUTRITIONIST_ID, THREAD_ID, input);

		assertThat(result.success()).isTrue();
		assertThat(result.data().draftId()).isEqualTo(88L);
		assertThat(result.data().draftType()).isEqualTo(AiDraftType.MENU);
		assertThat(result.data().status()).isEqualTo(AiDraftStatus.DRAFT);
		assertThat(result.data().summary()).contains("Menú balanceado");
		verify(draftLifecycleService).createDraft(eq(THREAD_ID), eq(NUTRITIONIST_ID), eq(AiDraftType.MENU), any());
	}

	@Test
	void createDraftRejectsEmptyIngestas() {
		final MenuDraftInput input = new MenuDraftInput(null, null, List.of(), null, null, null);

		final AiToolResult<AiDraftCreationData> result = service.createDraft(NUTRITIONIST_ID, THREAD_ID, input);

		assertThat(result.success()).isFalse();
		assertThat(result.errorCode()).isEqualTo(AiToolErrorCode.VALIDATION);
		verify(ingestaNutrientCalculator, never()).computeIngestas(any(), any());
		verify(draftLifecycleService, never()).createDraft(any(), any(), any(), any());
	}

	@Test
	void createDraftPropagatesNutrientComputationErrors() {
		when(ingestaNutrientCalculator.computeIngestas(eq(NUTRITIONIST_ID), any()))
			.thenReturn(AiToolResult.error(AiToolErrorCode.NOT_FOUND, "No se encontró el platillo solicitado."));

		final MenuDraftInput input = new MenuDraftInput(null, null, List
			.of(new IngestaSlotInput("Comida", 1, List.of(new IngestaSlotItemInput("PLATILLO", 99L, null, 1, null)))),
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
		when(draftLifecycleService.createDraft(eq(THREAD_ID), eq(NUTRITIONIST_ID), eq(AiDraftType.MENU), any()))
			.thenThrow(new AiDraftLifecycleException("Conversación no encontrada."));

		final MenuDraftInput input = new MenuDraftInput(null, null, List
			.of(new IngestaSlotInput("Cena", 1, List.of(new IngestaSlotItemInput("ALIMENTO", null, 1L, 1, null)))),
				null, null, null);
		final AiToolResult<AiDraftCreationData> result = service.createDraft(NUTRITIONIST_ID, THREAD_ID, input);

		assertThat(result.success()).isFalse();
		assertThat(result.errorCode()).isEqualTo(AiToolErrorCode.NOT_FOUND);
	}

}
