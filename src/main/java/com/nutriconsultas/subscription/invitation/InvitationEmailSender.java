package com.nutriconsultas.subscription.invitation;

import com.nutriconsultas.subscription.PlanTier;

/**
 * Sends nutritionist invitation emails. Templates must not contain PHI.
 */
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface InvitationEmailSender {

	void sendNutritionistInvitation(String recipientEmail, PlanTier planTier, String inviteUrl);

}
