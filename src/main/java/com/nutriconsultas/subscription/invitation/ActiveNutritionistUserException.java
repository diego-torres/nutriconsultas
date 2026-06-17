package com.nutriconsultas.subscription.invitation;

/**
 * Thrown when a platform admin tries to invite an email that already redeemed an invitation and
 * still has active subscription access.
 */
public final class ActiveNutritionistUserException extends RuntimeException {

	private final long redeemedInvitationId;

	public ActiveNutritionistUserException(final long redeemedInvitationId) {
		super("Email already has active nutritionist access");
		this.redeemedInvitationId = redeemedInvitationId;
	}

	public long getRedeemedInvitationId() {
		return redeemedInvitationId;
	}

}
