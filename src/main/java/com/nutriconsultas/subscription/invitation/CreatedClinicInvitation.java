package com.nutriconsultas.subscription.invitation;

/**
 * Result of creating a clinic director invitation.
 */
public record CreatedClinicInvitation(long invitationId, String inviteUrl) {
}
