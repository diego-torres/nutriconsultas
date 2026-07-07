package com.nutriconsultas.auth.apple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppleSignInPropertiesTest {

	@Test
	void webhookDisabledByDefault() {
		final AppleSignInProperties properties = new AppleSignInProperties();

		assertThat(properties.isWebhookEnabled()).isFalse();
		assertThat(properties.isAutoProcessDestructiveEvents()).isFalse();
		assertThat(properties.getExpectedIssuer()).isEqualTo("https://appleid.apple.com");
		assertThat(properties.getJwksUrl()).isEqualTo("https://appleid.apple.com/auth/keys");
		assertThat(properties.isWebhookConfigured()).isFalse();
	}

	@Test
	void trimsExpectedAudienceWhenPresent() {
		final AppleSignInProperties properties = new AppleSignInProperties();
		properties.setExpectedAudience("  com.minutriporcion.app  ");

		assertThat(properties.getExpectedAudience()).isEqualTo("com.minutriporcion.app");
		assertThat(properties.isWebhookConfigured()).isTrue();
	}

	@Test
	void ignoresBlankExpectedAudience() {
		final AppleSignInProperties properties = new AppleSignInProperties();
		properties.setExpectedAudience("   ");

		assertThat(properties.getExpectedAudience()).isEmpty();
		assertThat(properties.isWebhookConfigured()).isFalse();
	}

	@Test
	void configValidationFailsWhenWebhookEnabledWithoutAudience() {
		final AppleSignInProperties properties = new AppleSignInProperties();
		properties.getWebhook().setEnabled(true);

		assertThatThrownBy(() -> new AppleSignInConfig(properties).validateConfiguration())
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("APPLE_SIGNIN_EXPECTED_AUDIENCE");
	}

}
