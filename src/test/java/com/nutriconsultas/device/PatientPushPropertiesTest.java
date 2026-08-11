package com.nutriconsultas.device;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PatientPushPropertiesTest {

	@Test
	void isOperational_requiresEnabledAndAtLeastOnePlatform() {
		final PatientPushProperties properties = new PatientPushProperties();
		assertThat(properties.isOperational()).isFalse();

		properties.setEnabled(true);
		assertThat(properties.isEnabledButMisconfigured()).isTrue();
		assertThat(properties.isOperational()).isFalse();

		properties.getApns().setKeyId("KEY");
		properties.getApns().setTeamId("TEAM");
		properties.getApns().setBundleId("com.example.app");
		properties.getApns().setP8Key("-----BEGIN PRIVATE KEY-----abc-----END PRIVATE KEY-----");
		assertThat(properties.isApnsConfigured()).isTrue();
		assertThat(properties.isOperational()).isTrue();
	}

	@Test
	void isFcmConfigured_requiresProjectAndServiceAccountJson() {
		final PatientPushProperties properties = new PatientPushProperties();
		properties.getFcm().setProjectId("demo-project");
		assertThat(properties.isFcmConfigured()).isFalse();
		properties.getFcm().setServiceAccountJson("{\"type\":\"service_account\"}");
		assertThat(properties.isFcmConfigured()).isTrue();
	}

	@Test
	void resolveHost_dependsOnProductionFlag() {
		final PatientPushProperties.Apns apns = new PatientPushProperties.Apns();
		assertThat(apns.resolveHost()).isEqualTo("api.sandbox.push.apple.com");
		apns.setProduction(true);
		assertThat(apns.resolveHost()).isEqualTo("api.push.apple.com");
	}

}
