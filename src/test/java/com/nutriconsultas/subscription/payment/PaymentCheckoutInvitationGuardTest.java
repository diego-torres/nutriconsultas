package com.nutriconsultas.subscription.payment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.nutriconsultas.subscription.InvitationStatus;
import com.nutriconsultas.subscription.NutritionistInvitation;
import com.nutriconsultas.subscription.PlanTier;
import com.nutriconsultas.subscription.Subscription;
import com.nutriconsultas.subscription.SubscriptionStatus;

class PaymentCheckoutInvitationGuardTest {

	@Test
	void verifyEligibleForCheckout_acceptsPendingInvitation() {
		final NutritionistInvitation invitation = new NutritionistInvitation();
		invitation.setStatus(InvitationStatus.PENDING);

		assertThatCode(() -> PaymentCheckoutInvitationGuard.verifyEligibleForCheckout(invitation))
			.doesNotThrowAnyException();
	}

	@Test
	void verifyEligibleForCheckout_acceptsRedeemedInvitationWithPendingPaymentSubscription() {
		final Subscription subscription = new Subscription();
		subscription.setStatus(SubscriptionStatus.PENDING_PAYMENT);
		final NutritionistInvitation invitation = new NutritionistInvitation();
		invitation.setStatus(InvitationStatus.REDEEMED);
		invitation.setSubscription(subscription);

		assertThatCode(() -> PaymentCheckoutInvitationGuard.verifyEligibleForCheckout(invitation))
			.doesNotThrowAnyException();
	}

	@Test
	void verifyEligibleForCheckout_rejectsRedeemedInvitationWithoutPendingSubscription() {
		final NutritionistInvitation invitation = new NutritionistInvitation();
		invitation.setStatus(InvitationStatus.REDEEMED);
		invitation.setPlanTier(PlanTier.BASICO);

		assertThatThrownBy(() -> PaymentCheckoutInvitationGuard.verifyEligibleForCheckout(invitation))
			.isInstanceOf(PaymentProviderException.class)
			.hasMessageContaining("not pending checkout");
	}

}
