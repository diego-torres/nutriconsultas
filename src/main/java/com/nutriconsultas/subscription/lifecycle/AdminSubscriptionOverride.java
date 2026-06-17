package com.nutriconsultas.subscription.lifecycle;

/**
 * Platform admin override for subscription billing state.
 */
public record AdminSubscriptionOverride(Boolean paymentExempt, java.time.Instant periodEnd, Integer gracePeriodDays,
		com.nutriconsultas.subscription.SubscriptionStatus status, String reasonCode, String details) {

}
