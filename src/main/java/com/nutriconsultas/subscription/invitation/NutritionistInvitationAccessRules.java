package com.nutriconsultas.subscription.invitation;

import com.nutriconsultas.subscription.InvitationStatus;
import com.nutriconsultas.subscription.NutritionistInvitation;
import com.nutriconsultas.subscription.SubscriptionStatus;

/**
 * Shared rules for nutritionist invitation access and admin revoke eligibility.
 */
public final class NutritionistInvitationAccessRules {

	private NutritionistInvitationAccessRules() {
	}

	public static boolean blocksNewInvitation(final SubscriptionStatus status) {
		return status == SubscriptionStatus.PENDING_PAYMENT || status == SubscriptionStatus.TRIAL
				|| status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.GRACE;
	}

	public static boolean canRevokeAccess(final NutritionistInvitation invitation) {
		return invitation != null && invitation.getStatus() == InvitationStatus.REDEEMED
				&& invitation.getSubscription() != null
				&& invitation.getSubscription().getStatus() != SubscriptionStatus.CANCELLED;
	}

}
