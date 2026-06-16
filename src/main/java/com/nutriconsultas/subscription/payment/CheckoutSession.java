package com.nutriconsultas.subscription.payment;

/**
 * Checkout session returned by a payment provider. No card data is stored locally.
 */
public record CheckoutSession(Long subscriptionId, String checkoutUrl, String externalSubscriptionId,
		String externalCustomerId) {
}
