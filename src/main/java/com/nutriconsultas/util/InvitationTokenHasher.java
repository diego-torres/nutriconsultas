package com.nutriconsultas.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generates invitation tokens and stores SHA-256 hashes only. Verification uses
 * constant-time comparison.
 */
public final class InvitationTokenHasher {

	private static final int TOKEN_BYTES = 32;

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private InvitationTokenHasher() {
	}

	public static String generateToken() {
		final byte[] bytes = new byte[TOKEN_BYTES];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	public static String hashToken(final String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			throw new IllegalArgumentException("rawToken is required");
		}
		try {
			final MessageDigest digest = MessageDigest.getInstance("SHA-256");
			final byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return bytesToHex(hash);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 not available", ex);
		}
	}

	public static boolean verifyToken(final String rawToken, final String expectedHash) {
		if (rawToken == null || expectedHash == null) {
			return false;
		}
		final String actualHash = hashToken(rawToken);
		return MessageDigest.isEqual(actualHash.getBytes(StandardCharsets.UTF_8),
				expectedHash.getBytes(StandardCharsets.UTF_8));
	}

	private static String bytesToHex(final byte[] bytes) {
		final StringBuilder builder = new StringBuilder(bytes.length * 2);
		for (final byte value : bytes) {
			builder.append(String.format("%02x", value));
		}
		return builder.toString();
	}

}
