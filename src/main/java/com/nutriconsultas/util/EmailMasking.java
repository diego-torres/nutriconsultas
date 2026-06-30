package com.nutriconsultas.util;

/**
 * Masks email addresses for non-log API hints (#349). Never exposes the full local part.
 */
public final class EmailMasking {

	private EmailMasking() {
	}

	/**
	 * Returns a masked email suitable for optional mobile pre-fill hints (e.g.
	 * {@code p***@example.com}).
	 * @param email raw email address
	 * @return masked email, or {@code null} when blank or malformed
	 */
	public static String maskForHint(final String email) {
		if (email == null || email.isBlank()) {
			return null;
		}
		final String normalized = email.trim().toLowerCase();
		final int atIndex = normalized.indexOf('@');
		if (atIndex <= 0 || atIndex == normalized.length() - 1) {
			return null;
		}
		return normalized.charAt(0) + "***@" + normalized.substring(atIndex + 1);
	}

}
