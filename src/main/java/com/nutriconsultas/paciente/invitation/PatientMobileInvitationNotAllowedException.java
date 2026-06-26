package com.nutriconsultas.paciente.invitation;

/**
 * Patient is not eligible for mobile invitation actions (#341).
 */
public final class PatientMobileInvitationNotAllowedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final String messageKey;

	public PatientMobileInvitationNotAllowedException(final String messageKey) {
		super(messageKey);
		this.messageKey = messageKey;
	}

	public String getMessageKey() {
		return messageKey;
	}

}
