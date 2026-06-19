package com.nutriconsultas.admin;

/**
 * Platform-admin view of who owns or will own a
 * {@link com.nutriconsultas.subscription.Subscription}.
 */
public record SubscriptionOwnerView(String email, String userId, Long invitationId) {

	public boolean hasEmail() {
		return email != null && !email.isBlank();
	}

	public boolean hasUserId() {
		return userId != null && !userId.isBlank();
	}

}
