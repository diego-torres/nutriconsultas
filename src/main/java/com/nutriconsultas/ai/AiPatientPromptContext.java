package com.nutriconsultas.ai;

import java.util.Map;

/**
 * Redacted patient constraints merged into the AI system prompt (#367). Mirrors
 * {@code AiPatientContext} from {@code DATA-ACCESS-RULES.md} — no name, email, or phone.
 */
public record AiPatientPromptContext(Long patientId, Double requerimientoKcal, Double finalTotalKcal,
		Boolean physiologicalStressActive, String gender, Boolean pregnancy, String nivelPeso, Double imc,
		Map<String, Boolean> pathologyFlags, String alergias, String activityLevel, String nextAppointmentAtIso,
		String nextAppointmentTitle, Integer nextAppointmentDurationMinutes) {

	public AiPatientPromptContext {
		pathologyFlags = pathologyFlags == null ? Map.of() : Map.copyOf(pathologyFlags);
	}

}
