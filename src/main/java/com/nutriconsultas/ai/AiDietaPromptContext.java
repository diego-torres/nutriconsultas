package com.nutriconsultas.ai;

import java.util.List;

/**
 * Redacted diet plan context for the AI system prompt. No patient names.
 */
public record AiDietaPromptContext(Long dietaId, String nombre, Integer energiaKcal, Double proteinaGrams,
		Double lipidosGrams, Double hidratosDeCarbonoGrams, int ingestaCount, List<String> ingestaNames,
		boolean patientAssignment, Long linkedPatientId) {

	public AiDietaPromptContext {
		ingestaNames = ingestaNames == null ? List.of() : List.copyOf(ingestaNames);
	}

}
