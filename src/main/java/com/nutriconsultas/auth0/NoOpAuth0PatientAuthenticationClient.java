package com.nutriconsultas.auth0;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(Auth0PatientAuthenticationClient.class)
public class NoOpAuth0PatientAuthenticationClient implements Auth0PatientAuthenticationClient {

	@Override
	public boolean isConfigured() {
		return false;
	}

	@Override
	public void signUpDatabaseUser(final String email, final String password, final Map<String, String> userMetadata) {
		throw new IllegalStateException("Auth0 patient authentication broker is not configured");
	}

	@Override
	public Auth0PatientTokenResponse loginWithPassword(final String email, final String password,
			final String invitationToken) {
		throw new IllegalStateException("Auth0 patient authentication broker is not configured");
	}

}
