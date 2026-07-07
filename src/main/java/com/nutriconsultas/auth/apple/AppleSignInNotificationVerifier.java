package com.nutriconsultas.auth.apple;

/**
 * Verifies Apple server-to-server notification signed payloads (#501).
 */
public interface AppleSignInNotificationVerifier {

	AppleSignInNotificationClaims verifyAndParse(String signedPayload);

}
