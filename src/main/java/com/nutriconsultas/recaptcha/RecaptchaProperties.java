package com.nutriconsultas.recaptcha;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "recaptcha")
public class RecaptchaProperties {

	public static final String GOOGLE_TEST_SITE_KEY = "6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI"; // notsecret

	public static final String GOOGLE_TEST_SECRET_KEY = "6LeIxAcTAAAAAGG-vFI1TnRWxMZNFuojJ4WifJWe"; // notsecret

	private String siteKey = GOOGLE_TEST_SITE_KEY;

	private String secretKey = GOOGLE_TEST_SECRET_KEY;

	private boolean verificationEnabled = true;

	public String getSiteKey() {
		return siteKey;
	}

	public void setSiteKey(final String siteKey) {
		if (StringUtils.hasText(siteKey)) {
			this.siteKey = siteKey.trim();
		}
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(final String secretKey) {
		if (StringUtils.hasText(secretKey)) {
			this.secretKey = secretKey.trim();
		}
	}

	public boolean isVerificationEnabled() {
		return verificationEnabled;
	}

	public void setVerificationEnabled(final boolean verificationEnabled) {
		this.verificationEnabled = verificationEnabled;
	}

	public boolean isConfigured() {
		return StringUtils.hasText(siteKey) && StringUtils.hasText(secretKey);
	}

	public boolean isUsingGoogleTestKeys() {
		return GOOGLE_TEST_SITE_KEY.equals(siteKey) || GOOGLE_TEST_SECRET_KEY.equals(secretKey);
	}

}
