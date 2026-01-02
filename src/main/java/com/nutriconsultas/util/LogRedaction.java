package com.nutriconsultas.util;

/**
 * Utility class for redacting sensitive information from log messages. This class helps
 * prevent personal information from being exposed in logs.
 * <p>
 * <strong>Important:</strong> Never log patient-identifiable data such as:
 * <ul>
 * <li>Patient names</li>
 * <li>Email addresses</li>
 * <li>Phone numbers</li>
 * <li>Medical records or notes</li>
 * <li>Other sensitive personal information</li>
 * </ul>
 * <p>
 * Instead, log only non-sensitive identifiers like IDs, timestamps, and system
 * information.
 */
public final class LogRedaction {

	private LogRedaction() {
		// Utility class - prevent instantiation
	}

	/**
	 * Redacts a patient ID for logging purposes. Returns a safe string representation
	 * that only includes the ID.
	 * @param id the patient ID
	 * @return a safe string representation (e.g., "Paciente[id=123]")
	 */
	public static String redactPaciente(final Long id) {
		if (id == null) {
			return "Paciente[id=null]";
		}
		return "Paciente[id=" + id + "]";
	}

	/**
	 * Redacts patient information from an object for logging. Returns a safe string
	 * representation that only includes the ID.
	 * @param paciente the patient object (can be null)
	 * @return a safe string representation (e.g., "Paciente[id=123]")
	 */
	public static String redactPaciente(final Object paciente) {
		if (paciente == null) {
			return "Paciente[id=null]";
		}
		try {
			// Use reflection to get the ID field safely
			final java.lang.reflect.Method getIdMethod = paciente.getClass().getMethod("getId");
			final Object id = getIdMethod.invoke(paciente);
			return "Paciente[id=" + id + "]";
		}
		catch (Exception e) {
			// If reflection fails, return a generic safe representation
			return "Paciente[redacted]";
		}
	}

	/**
	 * Redacts a calendar event for logging purposes. Returns a safe string representation
	 * that only includes the event ID.
	 * @param eventId the event ID
	 * @return a safe string representation (e.g., "CalendarEvent[id=123]")
	 */
	public static String redactCalendarEvent(final Long eventId) {
		if (eventId == null) {
			return "CalendarEvent[id=null]";
		}
		return "CalendarEvent[id=" + eventId + "]";
	}

	/**
	 * Redacts calendar event information from an object for logging. Returns a safe
	 * string representation that only includes the event ID.
	 * @param event the calendar event object (can be null)
	 * @return a safe string representation (e.g., "CalendarEvent[id=123]")
	 */
	public static String redactCalendarEvent(final Object event) {
		if (event == null) {
			return "CalendarEvent[id=null]";
		}
		try {
			final java.lang.reflect.Method getIdMethod = event.getClass().getMethod("getId");
			final Object id = getIdMethod.invoke(event);
			return "CalendarEvent[id=" + id + "]";
		}
		catch (Exception e) {
			return "CalendarEvent[redacted]";
		}
	}

	/**
	 * Redacts a clinical exam for logging purposes. Returns a safe string representation
	 * that only includes the exam ID.
	 * @param examId the exam ID
	 * @return a safe string representation (e.g., "ClinicalExam[id=123]")
	 */
	public static String redactClinicalExam(final Long examId) {
		if (examId == null) {
			return "ClinicalExam[id=null]";
		}
		return "ClinicalExam[id=" + examId + "]";
	}

	/**
	 * Redacts clinical exam information from an object for logging. Returns a safe string
	 * representation that only includes the exam ID.
	 * @param exam the clinical exam object (can be null)
	 * @return a safe string representation (e.g., "ClinicalExam[id=123]")
	 */
	public static String redactClinicalExam(final Object exam) {
		if (exam == null) {
			return "ClinicalExam[id=null]";
		}
		try {
			final java.lang.reflect.Method getIdMethod = exam.getClass().getMethod("getId");
			final Object id = getIdMethod.invoke(exam);
			return "ClinicalExam[id=" + id + "]";
		}
		catch (Exception e) {
			return "ClinicalExam[redacted]";
		}
	}

	/**
	 * Redacts a PacienteDieta for logging purposes. Returns a safe string representation
	 * that only includes the assignment ID.
	 * @param pacienteDietaId the PacienteDieta ID
	 * @return a safe string representation (e.g., "PacienteDieta[id=123]")
	 */
	public static String redactPacienteDieta(final Long pacienteDietaId) {
		if (pacienteDietaId == null) {
			return "PacienteDieta[id=null]";
		}
		return "PacienteDieta[id=" + pacienteDietaId + "]";
	}

	/**
	 * Redacts PacienteDieta information from an object for logging. Returns a safe string
	 * representation that only includes the assignment ID.
	 * @param pacienteDieta the PacienteDieta object (can be null)
	 * @return a safe string representation (e.g., "PacienteDieta[id=123]")
	 */
	public static String redactPacienteDieta(final Object pacienteDieta) {
		if (pacienteDieta == null) {
			return "PacienteDieta[id=null]";
		}
		try {
			final java.lang.reflect.Method getIdMethod = pacienteDieta.getClass().getMethod("getId");
			final Object id = getIdMethod.invoke(pacienteDieta);
			return "PacienteDieta[id=" + id + "]";
		}
		catch (Exception e) {
			return "PacienteDieta[redacted]";
		}
	}

	/**
	 * Redacts an AnthropometricMeasurement for logging purposes. Returns a safe string
	 * representation that only includes the measurement ID.
	 * @param measurementId the measurement ID
	 * @return a safe string representation (e.g., "AnthropometricMeasurement[id=123]")
	 */
	public static String redactAnthropometricMeasurement(final Long measurementId) {
		if (measurementId == null) {
			return "AnthropometricMeasurement[id=null]";
		}
		return "AnthropometricMeasurement[id=" + measurementId + "]";
	}

	/**
	 * Redacts AnthropometricMeasurement information from an object for logging. Returns a
	 * safe string representation that only includes the measurement ID.
	 * @param measurement the AnthropometricMeasurement object (can be null)
	 * @return a safe string representation (e.g., "AnthropometricMeasurement[id=123]")
	 */
	public static String redactAnthropometricMeasurement(final Object measurement) {
		if (measurement == null) {
			return "AnthropometricMeasurement[id=null]";
		}
		try {
			final java.lang.reflect.Method getIdMethod = measurement.getClass().getMethod("getId");
			final Object id = getIdMethod.invoke(measurement);
			return "AnthropometricMeasurement[id=" + id + "]";
		}
		catch (Exception e) {
			return "AnthropometricMeasurement[redacted]";
		}
	}

}
