package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Map;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiSystemPromptServiceTest {

	private AiSystemPromptService service;

	@BeforeEach
	void setUp() {
		service = new AiSystemPromptServiceImpl();
	}

	@Test
	void defaultPromptIncludesCoreSafetyInstructionsInSpanish() {
		final String prompt = service.buildSystemPrompt(AiSystemPromptContext.defaultNutritionist());

		assertThat(prompt).contains("es-MX");
		assertThat(prompt.toLowerCase(Locale.ROOT)).contains("español");
		assertThat(prompt).contains(AiSystemPromptServiceImpl.SAFETY_MARKER_DRAFT_LABEL);
		assertThat(prompt.toLowerCase(Locale.ROOT)).contains(AiSystemPromptServiceImpl.SAFETY_MARKER_NO_ASSIGN);
		assertThat(prompt.toLowerCase(Locale.ROOT)).contains(AiSystemPromptServiceImpl.SAFETY_MARKER_CATALOG_TOOLS);
		assertThat(prompt.toLowerCase(Locale.ROOT)).contains(AiSystemPromptServiceImpl.SAFETY_MARKER_NO_CLINICAL_CLAIM);
		assertThat(prompt.toLowerCase(Locale.ROOT)).contains("no diagnostiques");
		assertThat(prompt.toLowerCase(Locale.ROOT)).contains("borrador");
	}

	@Test
	void promptIncludesNutritionistScopeHint() {
		final String prompt = service.buildSystemPrompt(new AiSystemPromptContext(Locale.forLanguageTag("es-MX"),
				"Solo catálogo del nutriólogo autenticado.", null, null, null));

		assertThat(prompt).contains("CONTEXTO DEL NUTRIÓLOGO");
		assertThat(prompt).contains("Solo catálogo del nutriólogo autenticado.");
	}

	@Test
	void promptIncludesPatientConstraintsWithoutReaskInstruction() {
		final AiPatientPromptContext patient = new AiPatientPromptContext(42L, 1800.0, 2000.0, true, "F", false,
				"NORMAL", 23.5, Map.of("hipertension", true, "diabetes", false), "Mariscos", "MODERATE");
		final String prompt = service
			.buildSystemPrompt(new AiSystemPromptContext(Locale.forLanguageTag("es-MX"), null, patient, null, null));

		assertThat(prompt).contains("CONTEXTO DEL PACIENTE");
		assertThat(prompt).contains("1800.0 kcal");
		assertThat(prompt).contains("2000.0 kcal");
		assertThat(prompt).contains("Mariscos");
		assertThat(prompt).contains("hipertension");
		assertThat(prompt).contains("No preguntes de nuevo el objetivo calórico ni las alergias");
		assertThat(prompt.toLowerCase(Locale.ROOT)).doesNotContain("maría");
	}

	@Test
	void promptIncludesDietaContext() {
		final AiDietaPromptContext dieta = new AiDietaPromptContext(7L, "Plan 1800", 1800, 90.0, 60.0, 200.0, 5,
				List.of("Desayuno", "Comida"), false, null);
		final String prompt = service
			.buildSystemPrompt(new AiSystemPromptContext(Locale.forLanguageTag("es-MX"), null, null, dieta, null));

		assertThat(prompt).contains("CONTEXTO DE LA DIETA EN PANTALLA");
		assertThat(prompt).contains("Plan 1800");
		assertThat(prompt).contains("Desayuno");
	}

	@Test
	void promptIncludesPlatilloContext() {
		final AiPlatilloPromptContext platillo = new AiPlatilloPromptContext(3L, "Ensalada verde", "Fresca y ligera",
				250, 4, List.of("Lechuga (1 taza)"), "Comida");
		final String prompt = service
			.buildSystemPrompt(new AiSystemPromptContext(Locale.forLanguageTag("es-MX"), null, null, null, platillo));

		assertThat(prompt).contains("CONTEXTO DEL PLATILLO EN PANTALLA");
		assertThat(prompt).contains("Ensalada verde");
		assertThat(prompt).contains("Lechuga");
	}

	@Test
	void nullContextUsesDefaults() {
		final String prompt = service.buildSystemPrompt(null);

		assertThat(prompt).contains("es-MX");
		assertThat(prompt).contains(AiSystemPromptServiceImpl.SAFETY_MARKER_DRAFT_LABEL);
	}

}
