package com.nutriconsultas.mobile;

/**
 * Thrown when an Auth0 {@code sub} is already assigned to a different {@code Paciente}.
 */
public class PatientAuthSubAlreadyLinkedException extends RuntimeException {

	public PatientAuthSubAlreadyLinkedException() {
		super("patient_auth_sub_already_linked");
	}

}
