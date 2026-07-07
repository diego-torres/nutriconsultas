package com.nutriconsultas.auth.apple;

/**
 * Verified Apple server-to-server notification claims (#501).
 */
public record AppleSignInNotificationClaims(String eventId, String issuer, String audience, String appleSubject,
		AppleSignInEventType eventType, String email, Boolean emailVerified, Boolean isPrivateEmail,
		String rawClaimsJson) {
}
