package com.nutriconsultas.paciente;

import org.springframework.lang.NonNull;

/**
 * Deletes a nutritionist-owned patient and all in-app clinical history (#223).
 */
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface PacienteDeletionService {

	/**
	 * Removes the patient row and related history when owned by {@code userId}.
	 * @param pacienteId patient primary key
	 * @param userId nutritionist OAuth {@code sub}
	 * @throws IllegalArgumentException when the patient is not found for the tenant
	 */
	void deletePatientWithHistory(@NonNull Long pacienteId, @NonNull String userId);

}
