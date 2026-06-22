package com.nutriconsultas.subscription.invitation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NutritionistInvitationPropertiesTest {

	@Test
	void buildRedeemUrlUsesConfiguredBaseUrl() {
		final NutritionistInvitationProperties properties = new NutritionistInvitationProperties();
		properties.setBaseUrl("https://minutriporcion.com");

		assertThat(properties.buildRedeemUrl("abc123"))
			.isEqualTo("https://minutriporcion.com/invitation/nutritionist/redeem?token=abc123");
	}

	@Test
	void buildRedeemUrlStripsTrailingSlashFromBaseUrl() {
		final NutritionistInvitationProperties properties = new NutritionistInvitationProperties();
		properties.setBaseUrl("https://minutriporcion.com/");

		assertThat(properties.buildRedeemUrl("abc123"))
			.isEqualTo("https://minutriporcion.com/invitation/nutritionist/redeem?token=abc123");
	}

	@Test
	void buildClinicRedeemUrlUsesConfiguredBaseUrl() {
		final NutritionistInvitationProperties properties = new NutritionistInvitationProperties();
		properties.setBaseUrl("https://minutriporcion.com");

		assertThat(properties.buildClinicRedeemUrl("abc123"))
			.isEqualTo("https://minutriporcion.com/invitation/clinic/redeem?token=abc123");
	}

}
