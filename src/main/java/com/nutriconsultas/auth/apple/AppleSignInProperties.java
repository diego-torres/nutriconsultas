package com.nutriconsultas.auth.apple;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.StringUtils;

/**
 * Apple Sign-In server-to-server notification configuration (#498).
 */
@ConfigurationProperties(prefix = "nutriconsultas.apple.signin")
public class AppleSignInProperties {

	private static final String DEFAULT_ISSUER = "https://appleid.apple.com";

	private static final String DEFAULT_JWKS_URL = "https://appleid.apple.com/auth/keys";

	@NestedConfigurationProperty
	private final Webhook webhook = new Webhook();

	private String expectedIssuer = DEFAULT_ISSUER;

	private String expectedAudience = "";

	private String jwksUrl = DEFAULT_JWKS_URL;

	private boolean autoProcessDestructiveEvents;

	private Duration jwksCacheTtl = Duration.ofHours(6);

	public Webhook getWebhook() {
		return webhook;
	}

	public boolean isWebhookEnabled() {
		return webhook.isEnabled();
	}

	public String getExpectedIssuer() {
		return expectedIssuer;
	}

	public void setExpectedIssuer(final String expectedIssuer) {
		if (StringUtils.hasText(expectedIssuer)) {
			this.expectedIssuer = expectedIssuer.trim();
		}
		else {
			this.expectedIssuer = DEFAULT_ISSUER;
		}
	}

	public String getExpectedAudience() {
		return expectedAudience;
	}

	public void setExpectedAudience(final String expectedAudience) {
		if (StringUtils.hasText(expectedAudience)) {
			this.expectedAudience = expectedAudience.trim();
		}
		else {
			this.expectedAudience = "";
		}
	}

	public String getJwksUrl() {
		return jwksUrl;
	}

	public void setJwksUrl(final String jwksUrl) {
		if (StringUtils.hasText(jwksUrl)) {
			this.jwksUrl = jwksUrl.trim();
		}
		else {
			this.jwksUrl = DEFAULT_JWKS_URL;
		}
	}

	public boolean isAutoProcessDestructiveEvents() {
		return autoProcessDestructiveEvents;
	}

	public void setAutoProcessDestructiveEvents(final boolean autoProcessDestructiveEvents) {
		this.autoProcessDestructiveEvents = autoProcessDestructiveEvents;
	}

	public Duration getJwksCacheTtl() {
		return jwksCacheTtl;
	}

	public void setJwksCacheTtl(final Duration jwksCacheTtl) {
		if (jwksCacheTtl != null && !jwksCacheTtl.isNegative() && !jwksCacheTtl.isZero()) {
			this.jwksCacheTtl = jwksCacheTtl;
		}
	}

	public boolean isWebhookConfigured() {
		return StringUtils.hasText(expectedAudience);
	}

	public static class Webhook {

		private boolean enabled;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(final boolean enabled) {
			this.enabled = enabled;
		}

	}

}
