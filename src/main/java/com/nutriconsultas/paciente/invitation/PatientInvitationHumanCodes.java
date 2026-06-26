package com.nutriconsultas.paciente.invitation;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Validates patient invitation human-readable code shape (#336). Codes follow
 * {@code PREFIX-XXXX-XXXX} Crockford-style encoding with a checksum digit.
 */
public final class PatientInvitationHumanCodes {

	private static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";

	private PatientInvitationHumanCodes() {
	}

	public static String normalize(final String rawCode) {
		if (rawCode == null) {
			return null;
		}
		return rawCode.trim().toUpperCase(Locale.ROOT);
	}

	public static boolean isWellFormed(final String rawCode, final String prefix) {
		final String normalized = normalize(rawCode);
		if (normalized == null || normalized.isBlank()) {
			return false;
		}
		final String normalizedPrefix = prefix == null || prefix.isBlank() ? "NUTRI" : prefix.trim().toUpperCase();
		final Pattern pattern = Pattern.compile("^" + Pattern.quote(normalizedPrefix) + "-[0-9A-Z]{4}-[0-9A-Z]{4}$");
		if (!pattern.matcher(normalized).matches()) {
			return false;
		}
		final int dashIndex = normalized.indexOf('-');
		final String payload = normalized.substring(dashIndex + 1).replace("-", "");
		if (payload.length() != 8) {
			return false;
		}
		for (int index = 0; index < 7; index++) {
			if (ALPHABET.indexOf(payload.charAt(index)) < 0) {
				return false;
			}
		}
		return payload.charAt(7) == checksum(payload.substring(0, 7).toCharArray());
	}

	private static char checksum(final char[] payload) {
		int sum = 0;
		for (int index = 0; index < 7; index++) {
			sum += ALPHABET.indexOf(payload[index]);
		}
		return ALPHABET.charAt(sum % ALPHABET.length());
	}

}
