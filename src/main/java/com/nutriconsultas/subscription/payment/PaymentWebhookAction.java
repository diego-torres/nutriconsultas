package com.nutriconsultas.subscription.payment;

/**
 * Normalized payment webhook actions supported in v1.
 */
public enum PaymentWebhookAction {

	PAYMENT_SUCCEEDED, PAYMENT_FAILED, SUBSCRIPTION_CANCELLED, IGNORED

}
