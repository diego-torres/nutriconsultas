package com.nutriconsultas.subscription.payment;

/**
 * Result of processing a payment webhook.
 */
public record PaymentWebhookResult(PaymentWebhookOutcome outcome, Long subscriptionId) {

	public static PaymentWebhookResult duplicate() {
		return new PaymentWebhookResult(PaymentWebhookOutcome.DUPLICATE, null);
	}

	public static PaymentWebhookResult processed(final Long subscriptionId) {
		return new PaymentWebhookResult(PaymentWebhookOutcome.PROCESSED, subscriptionId);
	}

	public static PaymentWebhookResult ignored() {
		return new PaymentWebhookResult(PaymentWebhookOutcome.IGNORED, null);
	}

}
