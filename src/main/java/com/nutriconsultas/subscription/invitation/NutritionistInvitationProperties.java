package com.nutriconsultas.subscription.invitation;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for platform-admin nutritionist invitations.
 */
@ConfigurationProperties(prefix = "nutriconsultas.subscription.invitation")
public class NutritionistInvitationProperties {

	private int expiryDays = 7;

	private String baseUrl = "http://localhost:3000";

	public int getExpiryDays() {
		return expiryDays;
	}

	public void setExpiryDays(final int expiryDays) {
		this.expiryDays = expiryDays;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(final String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String buildRedeemUrl(final String rawToken) {
		final String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
		return normalizedBase + "/invitation/nutritionist/redeem?token=" + rawToken;
	}

	public String buildClinicRedeemUrl(final String rawToken) {
		final String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
		return normalizedBase + "/invitation/clinic/redeem?token=" + rawToken;
	}

}
