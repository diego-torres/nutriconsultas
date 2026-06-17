package com.nutriconsultas.subscription.payment;

/**
 * Thrown when a payment webhook fails signature verification or parsing.
 */
public class InvalidPaymentWebhookException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public InvalidPaymentWebhookException(final String message) {
		super(message);
	}

	public InvalidPaymentWebhookException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
