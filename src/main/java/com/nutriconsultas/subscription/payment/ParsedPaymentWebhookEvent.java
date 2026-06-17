package com.nutriconsultas.subscription.payment;

import com.nutriconsultas.subscription.SubscriptionStatus;

/**
 * Normalized webhook event parsed from a provider payload.
 */
public record ParsedPaymentWebhookEvent(String eventId, String eventType, String externalSubscriptionId,
		String externalCustomerId, PaymentWebhookAction action, SubscriptionStatus targetStatus) {
}
