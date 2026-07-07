package com.nutriconsultas.auth.apple;

/**
 * Thrown when an Apple server-to-server notification fails verification or parsing
 * (#501).
 */
public class InvalidAppleSignInNotificationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public InvalidAppleSignInNotificationException(final String message) {
		super(message);
	}

	public InvalidAppleSignInNotificationException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
