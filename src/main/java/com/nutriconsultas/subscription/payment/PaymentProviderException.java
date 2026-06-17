package com.nutriconsultas.subscription.payment;

/**
 * Thrown when a payment provider operation fails.
 */
public class PaymentProviderException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PaymentProviderException(final String message) {
		super(message);
	}

	public PaymentProviderException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
