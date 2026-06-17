package com.nutriconsultas.subscription.invitation;

import com.nutriconsultas.subscription.PlanTier;

/**
 * Result of creating a platform-admin nutritionist invitation.
 */
public record CreatedNutritionistInvitation(Long invitationId, String inviteUrl) {

}
