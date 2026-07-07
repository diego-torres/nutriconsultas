package com.nutriconsultas.auth0;

import org.springframework.util.StringUtils;

/**
 * Auth0 user_id conventions for Sign in with Apple (#504).
 */
public final class Auth0AppleIdentitySupport {

	private static final String APPLE_PROVIDER = "apple";

	private Auth0AppleIdentitySupport() {
	}

	public static String toAuth0UserId(final String appleSubject) {
		if (!StringUtils.hasText(appleSubject)) {
			return null;
		}
		return APPLE_PROVIDER + "|" + appleSubject.trim();
	}

	public static boolean isAppleAuth0UserId(final String auth0UserId) {
		return StringUtils.hasText(auth0UserId) && auth0UserId.startsWith(APPLE_PROVIDER + "|");
	}

}
