package com.nutriconsultas.subscription.payment;

import com.nutriconsultas.subscription.SubscriptionStatus;

/**
 * Normalized webhook event parsed from a provider payload.
 *
 * @param externalSubscriptionId lookup key in {@code Subscription.externalSubscriptionId}
 * @param providerSubscriptionId when set, persisted as the canonical provider
 * subscription id
 */
public record ParsedPaymentWebhookEvent(String eventId, String eventType, String externalSubscriptionId,
		String externalCustomerId, String providerSubscriptionId, PaymentWebhookAction action,
		SubscriptionStatus targetStatus) {
}
