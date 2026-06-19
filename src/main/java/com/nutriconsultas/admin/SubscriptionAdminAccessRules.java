package com.nutriconsultas.admin;

import com.nutriconsultas.subscription.Subscription;
import com.nutriconsultas.subscription.SubscriptionStatus;

public final class SubscriptionAdminAccessRules {

	private SubscriptionAdminAccessRules() {
	}

	public static boolean isEditable(final Subscription subscription) {
		return subscription != null && subscription.getStatus() != SubscriptionStatus.CANCELLED;
	}

}
