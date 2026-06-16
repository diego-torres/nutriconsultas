package com.nutriconsultas.auth0;

import org.springframework.util.StringUtils;

final class Auth0DomainNormalizer {

	private Auth0DomainNormalizer() {
	}

	static String normalize(final String rawDomain) {
		if (!StringUtils.hasText(rawDomain)) {
			return "";
		}
		String normalized = rawDomain.trim();
		if (normalized.startsWith("https://")) {
			normalized = normalized.substring("https://".length());
		}
		else if (normalized.startsWith("http://")) {
			normalized = normalized.substring("http://".length());
		}
		while (normalized.endsWith("/")) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}
		return normalized;
	}

}
