package com.nutriconsultas.auth0;

import java.util.Optional;

/**
 * Resolves Auth0 user identifiers ({@code sub}) by email via the Management API.
 */
public interface Auth0UserLookup {

	boolean isConfigured();

	Optional<String> findUserIdByEmail(String email);

	/**
	 * Resolves the primary email for an Auth0 user id ({@code sub}) via the Management
	 * API.
	 */
	Optional<String> findEmailByUserId(String userId);

}
