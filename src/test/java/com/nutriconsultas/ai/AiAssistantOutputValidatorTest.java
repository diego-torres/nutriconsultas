package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiAssistantOutputValidatorTest {

	private AiAssistantOutputValidator validator;

	@BeforeEach
	void setUp() {
		validator = new AiAssistantOutputValidator();
	}

	@Test
	void leavesSafeNutritionContentUnchanged() {
		final String content = "Aquí tienes un borrador de menú bajo en sodio para revisión.";

		assertThat(validator.validateAndSanitize(content)).isEqualTo(content);
	}

	@Test
	void redactsOpenAiApiKey() {
		final String content = "La clave es sk-proj-abcdefghijklmnopqrstuvwxyz1234567890.";

		assertThat(validator.validateAndSanitize(content))
			.isEqualTo("La clave es " + AiAssistantOutputValidator.REDACTED_SECRET + ".");
	}

	@Test
	void redactsEmailAddress() {
		final String content = "Contacta a maria.garcia@example.com para más detalles.";

		assertThat(validator.validateAndSanitize(content))
			.isEqualTo("Contacta a " + AiAssistantOutputValidator.REDACTED_PII + " para más detalles.");
	}

	@Test
	void redactsAwsAccessKey() {
		final String content = "Key: AKIAIOSFODNN7EXAMPLE";

		assertThat(validator.validateAndSanitize(content))
			.isEqualTo("Key: " + AiAssistantOutputValidator.REDACTED_SECRET);
	}

}
