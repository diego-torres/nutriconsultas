package com.nutriconsultas.ai;

import java.util.Locale;

/**
 * Inputs for building the server-side AI system prompt (#367).
 */
public record AiSystemPromptContext(Locale locale, String nutritionistScopeHint,
		AiPatientPromptContext patientContext) {

	public static final String DEFAULT_LOCALE_TAG = "es-MX";

	public AiSystemPromptContext {
		if (locale == null) {
			locale = Locale.forLanguageTag(DEFAULT_LOCALE_TAG);
		}
	}

	public static AiSystemPromptContext defaultNutritionist() {
		return new AiSystemPromptContext(Locale.forLanguageTag(DEFAULT_LOCALE_TAG),
				"El nutriólogo autenticado es el único dueño de los datos y borradores de esta sesión.", null);
	}

}
