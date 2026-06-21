package com.nutriconsultas.recaptcha;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecaptchaVerificationServiceTest {

	private RecaptchaProperties properties;

	private RecaptchaVerificationService service;

	@BeforeEach
	void setUp() {
		properties = new RecaptchaProperties();
		service = new RecaptchaVerificationService(properties);
	}

	@Test
	void verifyTokenRejectsBlankToken() {
		assertThat(service.verifyToken(null)).isFalse();
		assertThat(service.verifyToken("")).isFalse();
		assertThat(service.verifyToken("   ")).isFalse();
	}

	@Test
	void verifyTokenRejectsWhenKeysNotConfigured() {
		properties.setSiteKey("");
		properties.setSecretKey("secret");

		assertThat(service.verifyToken("token")).isFalse();
	}

	@Test
	void verifyTokenSkipsWhenVerificationDisabled() {
		properties.setVerificationEnabled(false);

		assertThat(service.verifyToken("any-token")).isTrue();
	}

}
