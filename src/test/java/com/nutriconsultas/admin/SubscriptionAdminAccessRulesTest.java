package com.nutriconsultas.admin;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.nutriconsultas.subscription.Subscription;
import com.nutriconsultas.subscription.SubscriptionStatus;

class SubscriptionAdminAccessRulesTest {

	@Test
	void isEditable_falseWhenCancelled() {
		final Subscription subscription = new Subscription();
		subscription.setStatus(SubscriptionStatus.CANCELLED);

		assertThat(SubscriptionAdminAccessRules.isEditable(subscription)).isFalse();
	}

	@Test
	void isEditable_trueWhenActive() {
		final Subscription subscription = new Subscription();
		subscription.setStatus(SubscriptionStatus.ACTIVE);

		assertThat(SubscriptionAdminAccessRules.isEditable(subscription)).isTrue();
	}

}
