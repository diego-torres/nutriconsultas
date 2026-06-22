package com.nutriconsultas.paciente.invitation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PatientInvitationPropertiesTest {

	@Test
	void buildInviteUrlUsesConfiguredBaseUrl() {
		final PatientInvitationProperties properties = new PatientInvitationProperties();
		properties.setBaseUrl("https://links.minutriporcion.com");

		assertThat(properties.buildInviteUrl("abc123")).isEqualTo("https://links.minutriporcion.com/i/abc123");
	}

	@Test
	void buildInviteUrlStripsTrailingSlashFromBaseUrl() {
		final PatientInvitationProperties properties = new PatientInvitationProperties();
		properties.setBaseUrl("http://localhost:3000/");

		assertThat(properties.buildInviteUrl("abc123")).isEqualTo("http://localhost:3000/i/abc123");
	}

}
