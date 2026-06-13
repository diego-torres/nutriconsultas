package com.nutriconsultas.mobile;

/**
 * Thrown when email-based Auth0 lookup is requested but Management API credentials are
 * not configured.
 */
public class Auth0ManagementNotConfiguredException extends RuntimeException {

	public Auth0ManagementNotConfiguredException() {
		super("auth0_management_not_configured");
	}

}
