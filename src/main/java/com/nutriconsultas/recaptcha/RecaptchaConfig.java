package com.nutriconsultas.recaptcha;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableConfigurationProperties(RecaptchaProperties.class)
@Slf4j
public class RecaptchaConfig {

	private final RecaptchaProperties properties;

	public RecaptchaConfig(final RecaptchaProperties properties) {
		this.properties = properties;
	}

	@PostConstruct
	public void logRecaptchaConfiguration() {
		if (!properties.isConfigured()) {
			log.warn("reCAPTCHA is not fully configured (RECAPTCHA_SITE_KEY and/or RECAPTCHA_SECRET_KEY missing). "
					+ "Public forms will reject submissions until production keys are set.");
			return;
		}
		if (properties.isUsingGoogleTestKeys()) {
			log.warn("reCAPTCHA is using Google's public test keys. Register production keys for minutriporcion.com "
					+ "to remove the testing banner on public forms.");
		}
		else if (log.isInfoEnabled()) {
			log.info("reCAPTCHA production keys are configured for public forms");
		}
	}

}
