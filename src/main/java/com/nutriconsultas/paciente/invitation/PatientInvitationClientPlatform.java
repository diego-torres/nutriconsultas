package com.nutriconsultas.paciente.invitation;

/**
 * Client platform hint for invitation landing store badges (#337).
 */
public enum PatientInvitationClientPlatform {

	IOS, ANDROID, OTHER;

	public static PatientInvitationClientPlatform fromUserAgent(final String userAgent) {
		if (userAgent == null || userAgent.isBlank()) {
			return OTHER;
		}
		final String normalized = userAgent.toLowerCase();
		if (normalized.contains("iphone") || normalized.contains("ipad") || normalized.contains("ipod")) {
			return IOS;
		}
		if (normalized.contains("android")) {
			return ANDROID;
		}
		return OTHER;
	}

}
