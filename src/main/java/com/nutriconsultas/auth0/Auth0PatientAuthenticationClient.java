package com.nutriconsultas.auth0;

import java.util.Map;

/**
 * Server-side Auth0 Authentication API for patient database signup and login.
 */
public interface Auth0PatientAuthenticationClient {

	boolean isConfigured();

	void signUpDatabaseUser(String email, String password, Map<String, String> userMetadata);

	Auth0PatientTokenResponse loginWithPassword(String email, String password, String invitationToken);

}
