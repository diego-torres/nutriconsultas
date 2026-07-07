package com.nutriconsultas.auth.apple;

import org.springframework.util.StringUtils;

/**
 * Normalized Apple Sign-In server notification event types (#503).
 */
public enum AppleSignInEventType {

	EMAIL_DISABLED, EMAIL_ENABLED, CONSENT_REVOKED, ACCOUNT_DELETE, UNKNOWN;

	public static AppleSignInEventType fromAppleType(final String rawType) {
		if (!StringUtils.hasText(rawType)) {
			return UNKNOWN;
		}
		return switch (rawType.trim().toLowerCase()) {
			case "email-disabled" -> EMAIL_DISABLED;
			case "email-enabled" -> EMAIL_ENABLED;
			case "consent-revoked" -> CONSENT_REVOKED;
			case "account-delete" -> ACCOUNT_DELETE;
			default -> UNKNOWN;
		};
	}

	public boolean isDestructive() {
		return this == CONSENT_REVOKED || this == ACCOUNT_DELETE;
	}

}
