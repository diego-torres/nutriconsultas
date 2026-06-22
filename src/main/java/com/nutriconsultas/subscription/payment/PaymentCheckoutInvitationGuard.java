package com.nutriconsultas.subscription.payment;

import com.nutriconsultas.subscription.InvitationStatus;
import com.nutriconsultas.subscription.NutritionistInvitation;
import com.nutriconsultas.subscription.SubscriptionStatus;

/**
 * Shared validation for creating a payment checkout session after invitation redeem.
 */
public final class PaymentCheckoutInvitationGuard {

	private PaymentCheckoutInvitationGuard() {
	}

	public static void verifyEligibleForCheckout(final NutritionistInvitation invitation) {
		if (invitation.getStatus() == InvitationStatus.PENDING) {
			return;
		}
		if (invitation.getStatus() == InvitationStatus.REDEEMED && invitation.getSubscription() != null
				&& invitation.getSubscription().getStatus() == SubscriptionStatus.PENDING_PAYMENT) {
			return;
		}
		throw new PaymentProviderException("Invitation is not pending checkout");
	}

}
