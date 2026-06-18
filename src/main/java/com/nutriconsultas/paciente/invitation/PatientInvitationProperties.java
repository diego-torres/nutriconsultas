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

}
