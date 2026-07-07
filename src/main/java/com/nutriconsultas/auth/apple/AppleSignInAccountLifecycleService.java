package com.nutriconsultas.auth.apple;

/**
 * Staged Apple account deletion / access-revocation workflow (#506). Never hard-deletes
 * local patient data or Auth0 users.
 */
@FunctionalInterface
public interface AppleSignInAccountLifecycleService {

	AppleSignInLifecycleAction applyDestructiveEvent(AppleSignInNotification notification,
			AppleSignInEventType eventType);

}
