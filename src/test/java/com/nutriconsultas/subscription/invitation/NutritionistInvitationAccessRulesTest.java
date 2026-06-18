package com.nutriconsultas.subscription.invitation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.nutriconsultas.subscription.InvitationStatus;
import com.nutriconsultas.subscription.NutritionistInvitation;
import com.nutriconsultas.subscription.Subscription;
import com.nutriconsultas.subscription.SubscriptionStatus;

class NutritionistInvitationAccessRulesTest {

	@Test
	void blocksNewInvitationForActiveStatuses() {
		assertThat(NutritionistInvitationAccessRules.blocksNewInvitation(SubscriptionStatus.ACTIVE)).isTrue();
		assertThat(NutritionistInvitationAccessRules.blocksNewInvitation(SubscriptionStatus.GRACE)).isTrue();
		assertThat(NutritionistInvitationAccessRules.blocksNewInvitation(SubscriptionStatus.TRIAL)).isTrue();
		assertThat(NutritionistInvitationAccessRules.blocksNewInvitation(SubscriptionStatus.PENDING_PAYMENT)).isTrue();
	}

	@Test
	void allowsNewInvitationForCancelledOrSuspended() {
		assertThat(NutritionistInvitationAccessRules.blocksNewInvitation(SubscriptionStatus.CANCELLED)).isFalse();
		assertThat(NutritionistInvitationAccessRules.blocksNewInvitation(SubscriptionStatus.SUSPENDED)).isFalse();
	}

	@Test
	void canRevokeAccessForRedeemedActiveSubscription() {
		final NutritionistInvitation invitation = new NutritionistInvitation();
		invitation.setStatus(InvitationStatus.REDEEMED);
		final Subscription subscription = new Subscription();
		subscription.setStatus(SubscriptionStatus.ACTIVE);
		invitation.setSubscription(subscription);

		assertThat(NutritionistInvitationAccessRules.canRevokeAccess(invitation)).isTrue();
	}

	@Test
	void cannotRevokeAccessWhenAlreadyCancelled() {
		final NutritionistInvitation invitation = new NutritionistInvitation();
		invitation.setStatus(InvitationStatus.REDEEMED);
		final Subscription subscription = new Subscription();
		subscription.setStatus(SubscriptionStatus.CANCELLED);
		invitation.setSubscription(subscription);

		assertThat(NutritionistInvitationAccessRules.canRevokeAccess(invitation)).isFalse();
	}

}
