package com.nutriconsultas.ai;

/**
 * Redacted calendar row exposed to the AI model (no patient name or clinical vitals).
 */
public record PatientAppointmentItem(long eventId, String eventDateTimeIso, String title, int durationMinutes,
		String status) {
}
