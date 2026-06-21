package com.nutriconsultas.booking;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves client IP for anonymous rate limiting (#248).
 */
public final class ClientIpResolver {

	private ClientIpResolver() {
	}

	public static String resolve(final HttpServletRequest request) {
		if (request == null) {
			return "unknown";
		}
		final String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

}
