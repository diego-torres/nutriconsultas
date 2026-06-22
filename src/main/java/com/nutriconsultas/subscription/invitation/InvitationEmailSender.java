package com.nutriconsultas.subscription.invitation;

import com.nutriconsultas.subscription.PlanTier;

/**
 * Sends nutritionist invitation emails. Templates must not contain PHI.
 */
public interface InvitationEmailSender {

	void sendNutritionistInvitation(String recipientEmail, PlanTier planTier, String inviteUrl);

	void sendClinicInvitation(String recipientEmail, String clinicName, String inviteUrl);

}
