package com.nutriconsultas.auth.apple;

/**
 * Apple server-to-server notification body ({@code payload} is a signed JWT string).
 */
public record AppleSignInWebhookRequest(String payload) {
}
