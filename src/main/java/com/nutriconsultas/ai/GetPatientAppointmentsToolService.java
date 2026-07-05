package com.nutriconsultas.ai;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * AI tool {@code get_patient_appointments} — read-only calendar for the patient linked to
 * the thread.
 */
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface GetPatientAppointmentsToolService {

	String TOOL_NAME = "get_patient_appointments";

	AiToolResult<PatientAppointmentsData> getAppointments(@NonNull String nutritionistId, @NonNull Long patientId,
			@Nullable PatientAppointmentScope scope, @Nullable Integer limit);

}
