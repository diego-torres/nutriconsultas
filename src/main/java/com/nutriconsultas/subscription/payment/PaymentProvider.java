package com.nutriconsultas.subscription.payment;

import com.nutriconsultas.subscription.PlanTier;

/**
 * Payment provider abstraction for checkout, webhooks, and subscription cancellation.
 */
public interface PaymentProvider {

	String getProviderId();

	CheckoutSession createCheckoutSession(Long invitationId, PlanTier planTier, BillingInterval billingInterval);

	boolean verifyWebhookSignature(String payload, PaymentWebhookHeaders headers);

	ParsedPaymentWebhookEvent parseWebhook(String payload, PaymentWebhookHeaders headers);

	void cancelSubscription(String externalSubscriptionId);

}
