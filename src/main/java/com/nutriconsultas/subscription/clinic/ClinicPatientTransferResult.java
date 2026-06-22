package com.nutriconsultas.subscription.clinic;

/**
 * Outcome of a director-initiated patient transfer within the clinic.
 */
public record ClinicPatientTransferResult(int transferredCount, String warningMessage) {

	public boolean hasWarning() {
		return warningMessage != null && !warningMessage.isBlank();
	}

}
