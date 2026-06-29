package com.nutriconsultas.auth0;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Auth0 Authentication API failure mapped for mobile auth broker endpoints.
 */
public final class Auth0PatientAuthenticationException extends ResponseStatusException {

	public static final String CODE_INVALID_CREDENTIALS = "invalid_credentials";

	public static final String CODE_EMAIL_IN_USE = "email_in_use";

	public static final String CODE_WEAK_PASSWORD = "weak_password";

	public static final String CODE_INVITATION_REQUIRED = "invitation_required";

	public static final String CODE_AUTH0_ERROR = "auth0_error";

	private final String errorCode;

	public Auth0PatientAuthenticationException(final HttpStatus status, final String errorCode, final String reason) {
		super(status, reason);
		this.errorCode = errorCode;
	}

	public String getErrorCode() {
		return errorCode;
	}

}
