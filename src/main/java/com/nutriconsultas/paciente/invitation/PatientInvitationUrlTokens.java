package com.nutriconsultas.paciente.invitation;

import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Validates patient invitation URL token shape (#133, #135). Tokens are 256-bit secrets
 * encoded as unpadded Base64url (43 characters).
 */
public final class PatientInvitationUrlTokens {

	private static final int TOKEN_BYTES = 32;

	private static final int ENCODED_LENGTH = 43;

	private static final Pattern BASE64URL = Pattern.compile("[A-Za-z0-9_-]{43}");

	private PatientInvitationUrlTokens() {
	}

	public static boolean isWellFormed(final String rawToken) {
		if (rawToken == null || rawToken.length() != ENCODED_LENGTH) {
			return false;
		}
		if (!BASE64URL.matcher(rawToken).matches()) {
			return false;
		}
		try {
			final byte[] decoded = Base64.getUrlDecoder().decode(rawToken);
			return decoded.length == TOKEN_BYTES;
		}
		catch (IllegalArgumentException ex) {
			return false;
		}
	}

}
