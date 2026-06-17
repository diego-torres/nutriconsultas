package com.nutriconsultas.subscription.invitation;

/**
 * Thrown when a platform admin tries to create an invitation for an email that already
 * has a pending invitation.
 */
public final class PendingNutritionistInvitationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final long existingInvitationId;

	public PendingNutritionistInvitationException(final long existingInvitationId) {
		super("Pending invitation already exists");
		this.existingInvitationId = existingInvitationId;
	}

	public long getExistingInvitationId() {
		return existingInvitationId;
	}

}
