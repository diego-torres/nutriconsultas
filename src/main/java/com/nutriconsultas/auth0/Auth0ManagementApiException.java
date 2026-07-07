package com.nutriconsultas.auth0;

/**
 * Auth0 Management API call failed after retries (#505).
 */
public class Auth0ManagementApiException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public Auth0ManagementApiException(final String message) {
		super(message);
	}

	public Auth0ManagementApiException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
