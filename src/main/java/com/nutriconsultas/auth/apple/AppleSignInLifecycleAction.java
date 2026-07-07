package com.nutriconsultas.auth.apple;

/**
 * Safe lifecycle action taken (or skipped) for an Apple Sign-In notification (#506).
 */
public enum AppleSignInLifecycleAction {

	NOT_APPLICABLE, SKIPPED_OBSERVE_ONLY, SKIPPED_NO_PACIENTE, APPLIED_ACCESS_REVOKED, APPLIED_PENDING_DELETION_REVIEW,
	ALREADY_APPLIED, AUTH0_UPDATE_FAILED

}
