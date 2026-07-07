package com.nutriconsultas.auth.apple;

import org.springframework.util.StringUtils;

/**
 * Apple Hide My Email / private relay conventions (#507).
 */
public final class AppleRelayEmailSupport {

	private static final String PRIVATE_RELAY_DOMAIN = "@privaterelay.appleid.com";

	private AppleRelayEmailSupport() {
	}

	public static boolean isApplePrivateRelayEmail(final String email) {
		return StringUtils.hasText(email) && email.trim().toLowerCase().endsWith(PRIVATE_RELAY_DOMAIN);
	}

}
