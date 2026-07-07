package com.nutriconsultas.auth.apple;

/**
 * Processing lifecycle for persisted Apple Sign-In notifications (#502).
 */
public enum AppleSignInNotificationProcessingStatus {

	RECEIVED, VERIFIED, IGNORED, PROCESSED, FAILED

}
