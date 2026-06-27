package com.nutriconsultas.mobile;

import java.util.Optional;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

/**
 * Resolves patient email from Auth0 JWT claims for invitation reconciliation.
 */
public final class MobileJwtEmailResolver {

	private MobileJwtEmailResolver() {
	}

	public static Optional<String> resolveEmail(final Jwt jwt) {
		if (jwt == null) {
			return Optional.empty();
		}
		final String email = jwt.getClaimAsString("email");
		if (StringUtils.hasText(email)) {
			return Optional.of(email.trim());
		}
		return Optional.empty();
	}

}
