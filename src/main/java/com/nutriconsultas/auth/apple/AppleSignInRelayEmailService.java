package com.nutriconsultas.auth.apple;

/**
 * Applies Apple private relay email change notifications (#507).
 */
public interface AppleSignInRelayEmailService {

	AppleSignInLifecycleAction applyRelayEmailEvent(AppleSignInNotification notification,
			AppleSignInNotificationClaims claims);

}
