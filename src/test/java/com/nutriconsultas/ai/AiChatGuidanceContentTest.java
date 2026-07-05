package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Ensures in-app nutritionist guidance is present on the AI chat page (#407).
 */
class AiChatGuidanceContentTest {

	private static final String TEMPLATE = "templates/sbadmin/ai/chat.html";

	@Test
	void chatTemplateIncludesDraftReviewGuidance() throws IOException {
		final String html = loadTemplate();

		assertThat(html).contains("Cómo usar el asistente");
		assertThat(html).contains("Borrador IA — revisión del nutriólogo requerida");
		assertThat(html).contains("no asigna");
	}

	@Test
	void chatTemplateIncludesPromptExamplesAndLimitations() throws IOException {
		final String html = loadTemplate();

		assertThat(html).contains("Ejemplos de buenos prompts");
		assertThat(html).contains("Genera un desayuno alto en proteína");
		assertThat(html).contains("Limitaciones");
		assertThat(html).contains("No diagnostica");
	}

	@Test
	void chatTemplateDocumentsAcceptAndDiscardFlow() throws IOException {
		final String html = loadTemplate();

		assertThat(html).contains("Aceptar o descartar");
		assertThat(html).contains("id=\"aiChatDraftAcceptBtn\"");
		assertThat(html).contains("id=\"aiChatDraftDiscardBtn\"");
	}

	private static String loadTemplate() throws IOException {
		final ClassPathResource resource = new ClassPathResource(TEMPLATE);
		return resource.getContentAsString(StandardCharsets.UTF_8);
	}

}
