package com.nutriconsultas.paciente.invitation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Crockford-style human invitation codes (e.g. {@code NUTRI-7F3K-9Q2X}) derived from the
 * same CSPRNG secret as the URL token (#133).
 */
final class PatientInvitationHumanCode {

	private static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";

	private PatientInvitationHumanCode() {
	}

	static String fromUrlToken(final String rawUrlToken, final String prefix) {
		if (!PatientInvitationUrlTokens.isWellFormed(rawUrlToken)) {
			throw new IllegalArgumentException("invalid invitation URL token");
		}
		final byte[] secret = Base64.getUrlDecoder().decode(rawUrlToken);
		return format(secret, prefix);
	}

	static String format(final byte[] secret, final String prefix) {
		if (secret == null || secret.length < 5) {
			throw new IllegalArgumentException("secret must be at least 5 bytes");
		}
		final String normalizedPrefix = prefix == null || prefix.isBlank() ? "NUTRI" : prefix.trim().toUpperCase();
		final char[] payload = new char[8];
		int bitBuffer = 0;
		int bitCount = 0;
		int secretIndex = 0;
		for (int index = 0; index < 7; index++) {
			while (bitCount < 5) {
				final int byteValue = secret[secretIndex] & 0xFF;
				secretIndex++;
				bitBuffer = (bitBuffer << 8) | byteValue;
				bitCount += 8;
			}
			bitCount -= 5;
			final int value = (bitBuffer >> bitCount) & 0x1F;
			payload[index] = ALPHABET.charAt(value);
		}
		payload[7] = checksum(payload);
		return normalizedPrefix + "-" + new String(payload, 0, 4) + "-" + new String(payload, 4, 4);
	}

	static boolean matchesSecret(final byte[] secret, final String prefix, final String humanCode) {
		if (humanCode == null || humanCode.isBlank()) {
			return false;
		}
		final String expected = format(secret, prefix);
		return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
				humanCode.trim().toUpperCase().getBytes(StandardCharsets.UTF_8));
	}

	private static char checksum(final char... payload) {
		int sum = 0;
		for (int index = 0; index < 7; index++) {
			sum += ALPHABET.indexOf(payload[index]);
		}
		return ALPHABET.charAt(sum % ALPHABET.length());
	}

}
