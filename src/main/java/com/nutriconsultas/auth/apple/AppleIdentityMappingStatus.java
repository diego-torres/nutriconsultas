package com.nutriconsultas.auth.apple;

/**
 * Result of mapping an Apple notification subject to Auth0 and local accounts (#504).
 */
public enum AppleIdentityMappingStatus {

	NOT_ATTEMPTED, MAPPED, NO_APPLE_SUBJECT, NO_AUTH0_USER, NO_LOCAL_USER, AMBIGUOUS, AUTH0_LOOKUP_FAILED

}
