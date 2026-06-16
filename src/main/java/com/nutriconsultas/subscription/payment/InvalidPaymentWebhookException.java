package com.nutriconsultas.subscription.payment;

/**
 * Thrown when a payment webhook fails signature verification or parsing.
 */
public class InvalidPaymentWebhookException extends RuntimeException {

	public InvalidPaymentWebhookException(final String message) {
		super(message);
	}

}
