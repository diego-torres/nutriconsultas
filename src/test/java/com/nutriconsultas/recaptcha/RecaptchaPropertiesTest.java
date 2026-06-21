package com.nutriconsultas.recaptcha;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecaptchaPropertiesTest {

	@Test
	void isConfiguredWhenBothKeysPresent() {
		final RecaptchaProperties properties = new RecaptchaProperties();
		properties.setSiteKey("6Lprod-site-key");
		properties.setSecretKey("6Lprod-secret-key");

		assertThat(properties.isConfigured()).isTrue();
		assertThat(properties.isUsingGoogleTestKeys()).isFalse();
	}

	@Test
	void ignoresBlankSiteKeyOverride() {
		final RecaptchaProperties properties = new RecaptchaProperties();
		properties.setSiteKey("   ");

		assertThat(properties.getSiteKey()).isEqualTo(RecaptchaProperties.GOOGLE_TEST_SITE_KEY);
		assertThat(properties.isConfigured()).isTrue();
	}

	@Test
	void detectsGoogleTestKeys() {
		final RecaptchaProperties properties = new RecaptchaProperties();

		assertThat(properties.isUsingGoogleTestKeys()).isTrue();
		assertThat(properties.isConfigured()).isTrue();
	}

}
