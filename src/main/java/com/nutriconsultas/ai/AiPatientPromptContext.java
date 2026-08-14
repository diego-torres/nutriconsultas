package com.nutriconsultas.ai;

import java.util.Map;

/**
 * Patient constraints merged into the AI system prompt (#367). Includes display name for
 * nutritionist confirmation only; do not log this record.
 */
public record AiPatientPromptContext(Long patientId, Double requerimientoKcal, Double finalTotalKcal,
		Boolean physiologicalStressActive, String gender, Boolean pregnancy, String nivelPeso, Double imc,
		Map<String, Boolean> pathologyFlags, String alergias, String activityLevel, String nextAppointmentAtIso,
		String nextAppointmentTitle, Integer nextAppointmentDurationMinutes, String displayName) {

	public AiPatientPromptContext {
		pathologyFlags = pathologyFlags == null ? Map.of() : Map.copyOf(pathologyFlags);
	}

}
