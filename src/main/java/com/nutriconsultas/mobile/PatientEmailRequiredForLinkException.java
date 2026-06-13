package com.nutriconsultas.mobile;

/**
 * Thrown when email-based linkage is requested but the patient record has no email.
 */
public class PatientEmailRequiredForLinkException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PatientEmailRequiredForLinkException() {
		super("patient_email_required");
	}

}
