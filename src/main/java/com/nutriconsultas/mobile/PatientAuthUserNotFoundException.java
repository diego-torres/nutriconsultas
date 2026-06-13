package com.nutriconsultas.mobile;

/**
 * Thrown when Auth0 has no user for the patient email used in linkage.
 */
public class PatientAuthUserNotFoundException extends RuntimeException {

	public PatientAuthUserNotFoundException() {
		super("patient_auth_user_not_found");
	}

}
