package com.nutriconsultas.paciente.invitation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Configuration for patient onboarding invitations (#132–#141).
 */
@ConfigurationProperties(prefix = "nutriconsultas.patient.invitation")
public class PatientInvitationProperties {

	private int expiryDays = 14;

	private String humanCodePrefix = "NUTRI";

	/**
	 * HMAC secret for optional offline JWS (Auth0 Post-Login Action, #140). Empty
	 * disables JWS helpers.
	 */
	private String jwsSecret = "";

	/**
	 * Base URL for patient invite deep links ({@code {baseUrl}/i/{token}}). Production
	 * typically uses the links subdomain (e.g. {@code https://links.minutriporcion.com}).
	 */
	private String baseUrl = "http://localhost:3000";

	public int getExpiryDays() {
		return expiryDays;
	}

	public void setExpiryDays(final int expiryDays) {
		this.expiryDays = expiryDays;
	}

	public String getHumanCodePrefix() {
		return humanCodePrefix;
	}

	public void setHumanCodePrefix(final String humanCodePrefix) {
		this.humanCodePrefix = humanCodePrefix;
	}

	public String getJwsSecret() {
		return jwsSecret;
	}

	public void setJwsSecret(final String jwsSecret) {
		this.jwsSecret = jwsSecret;
	}

	public boolean isJwsEnabled() {
		return StringUtils.hasText(jwsSecret);
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(final String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String buildInviteUrl(final String rawUrlToken) {
		final String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
		return normalizedBase + "/i/" + rawUrlToken;
	}

}
