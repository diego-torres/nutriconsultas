package com.nutriconsultas.subscription.invitation;

/**
 * Result of redeeming a nutritionist invitation.
 */
public sealed interface RedeemNutritionistInvitationResult
		permits RedeemNutritionistInvitationResult.Activated, RedeemNutritionistInvitationResult.CheckoutRedirect {

	record Activated() implements RedeemNutritionistInvitationResult {
	}

	record CheckoutRedirect(String checkoutUrl) implements RedeemNutritionistInvitationResult {
	}

}
