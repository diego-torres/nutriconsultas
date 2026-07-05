package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AiDraftPreviewServiceTest {

	private static final String NUTRITIONIST_ID = "auth0|nutritionist-a";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@InjectMocks
	private AiDraftPreviewServiceImpl service;

	@Mock
	private AiGeneratedDraftRepository draftRepository;

	@Mock
	private AiEntitlementGuard aiEntitlementGuard;

	@org.junit.jupiter.api.BeforeEach
	void stubEntitlement() {
		org.mockito.Mockito.lenient()
			.doNothing()
			.when(aiEntitlementGuard)
			.assertCanUseAiAssistant(org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	void getPreviewReturnsDishDetails() throws Exception {
		final AiGeneratedDraft draft = dishDraft();
		when(draftRepository.findByIdAndThreadNutritionistId(10L, NUTRITIONIST_ID)).thenReturn(Optional.of(draft));

		final AiDraftPreviewView preview = service.getPreview(10L, NUTRITIONIST_ID);

		assertThat(preview.draftType()).isEqualTo(AiDraftType.DISH);
		assertThat(preview.title()).isEqualTo("Tacos de pollo");
		assertThat(preview.reviewLabel()).isEqualTo(AiDraftSummaryExtractor.REVIEW_LABEL);
		assertThat(preview.ingredients()).hasSize(1);
		assertThat(preview.preparationSteps()).containsExactly("Cocinar el pollo");
		assertThat(preview.assumptions()).containsExactly("Porción estándar");
		assertThat(preview.warnings()).containsExactly("Revisar sodio");
		assertThat(preview.portions()).isEqualTo(2);
		assertThat(preview.nutrients().energiaKcal()).isEqualTo(250);
	}

	@Test
	void getPreviewRejectsUnknownDraft() {
		when(draftRepository.findByIdAndThreadNutritionistId(99L, NUTRITIONIST_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getPreview(99L, NUTRITIONIST_ID)).isInstanceOf(AiDraftLifecycleException.class)
			.hasMessageContaining("Borrador no encontrado");
	}

	private static AiGeneratedDraft dishDraft() throws Exception {
		final DishDraftPayload payload = new DishDraftPayload("Tacos de pollo", "Receta ligera",
				List.of("Cocinar el pollo"), "Comida", List.of(new RecipeIngredientInput(1L, "1", null, "pz")), 2,
				new NutrientSummary(250, 20.0, 8.0, 30.0, 4.0, 300.0, 500.0), List.of("Porción estándar"),
				List.of("Revisar sodio"), AiDraftSummaryExtractor.REVIEW_LABEL);
		final AiChatThread thread = new AiChatThread();
		thread.setId(42L);
		final AiGeneratedDraft draft = new AiGeneratedDraft();
		draft.setId(10L);
		draft.setThread(thread);
		draft.setDraftType(AiDraftType.DISH);
		draft.setStatus(AiDraftStatus.DRAFT);
		draft.setJsonPayload(OBJECT_MAPPER.writeValueAsString(payload));
		return draft;
	}

}
