package com.nutriconsultas.subscription.invitation;

/**
 * Thrown when a director tries to invite an email that already has a pending clinic
 * invitation.
 */
public class PendingClinicInvitationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final long existingInvitationId;

	public PendingClinicInvitationException(final long existingInvitationId) {
		super("Pending clinic invitation already exists");
		this.existingInvitationId = existingInvitationId;
	}

	public long getExistingInvitationId() {
		return existingInvitationId;
	}

}
