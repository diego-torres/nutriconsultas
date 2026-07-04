package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiDraftToolSchemaValidatorTest {

	private AiDraftToolSchemaValidator validator;

	@BeforeEach
	void setUp() {
		validator = new AiDraftToolSchemaValidator();
	}

	@Test
	void acceptsValidDishDraftArguments() {
		final String json = """
				{
				  "name": "Tacos de pollo",
				  "ingredients": [
				    { "alimentoId": 1, "cantidad": "1" }
				  ],
				  "portions": 2
				}
				""";

		assertThat(validator.validateDishDraftArguments(json)).isEmpty();
	}

	@Test
	void rejectsDishDraftMissingRequiredName() {
		final String json = """
				{
				  "ingredients": [
				    { "alimentoId": 1, "cantidad": "1" }
				  ]
				}
				""";

		assertThat(validator.validateDishDraftArguments(json)).isPresent().get().asString().contains("obligatorio");
	}

	@Test
	void rejectsDishDraftWithAdditionalProperties() {
		final String json = """
				{
				  "name": "Ensalada",
				  "ingredients": [
				    { "alimentoId": 1, "cantidad": "1" }
				  ],
				  "unexpectedField": true
				}
				""";

		assertThat(validator.validateDishDraftArguments(json)).isPresent().get().asString().contains("no permitido");
	}

	@Test
	void rejectsMalformedJson() {
		assertThat(validator.validateMenuDraftArguments("{name:")).isPresent()
			.get()
			.isEqualTo(AiDraftToolSchemaValidator.INVALID_JSON_MESSAGE);
	}

	@Test
	void acceptsValidMenuDraftArguments() {
		final String json = """
				{
				  "title": "Menú bajo en sodio",
				  "targetKcal": 1800,
				  "ingestas": [
				    {
				      "nombre": "Desayuno",
				      "items": [
				        { "type": "ALIMENTO", "alimentoId": 10, "portions": 1 }
				      ]
				    }
				  ]
				}
				""";

		assertThat(validator.validateMenuDraftArguments(json)).isEmpty();
	}

	@Test
	void rejectsMenuDraftMissingIngestas() {
		final String json = """
				{
				  "title": "Menú incompleto"
				}
				""";

		assertThat(validator.validateMenuDraftArguments(json)).isPresent();
	}

	@Test
	void acceptsValidDietPlanDraftArguments() {
		final String json = """
				{
				  "title": "Plan 7 días",
				  "dayCount": 1,
				  "days": [
				    {
				      "dayIndex": 1,
				      "ingestas": [
				        {
				          "nombre": "Comida",
				          "items": [
				            { "type": "PLATILLO", "platilloId": 5, "portions": 1 }
				          ]
				        }
				      ]
				    }
				  ]
				}
				""";

		assertThat(validator.validateDietPlanDraftArguments(json)).isEmpty();
	}

	@Test
	void rejectsDietPlanDraftWithTooManyDays() {
		final StringBuilder days = new StringBuilder("[");
		for (int day = 1; day <= 15; day++) {
			if (day > 1) {
				days.append(',');
			}
			days.append("""
					{
					  "dayIndex": %d,
					  "ingestas": [
					    {
					      "items": [
					        { "type": "ALIMENTO", "alimentoId": 1, "portions": 1 }
					      ]
					    }
					  ]
					}
					""".formatted(day));
		}
		days.append(']');
		final String json = "{ \"days\": " + days + " }";

		assertThat(validator.validateDietPlanDraftArguments(json)).isPresent();
	}

}
