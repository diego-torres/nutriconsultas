package com.nutriconsultas.paciente.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PatientInvitationTokenServiceTest {

	private PatientInvitationTokenService service;

	@BeforeEach
	void setUp() {
		final PatientInvitationProperties properties = new PatientInvitationProperties();
		properties.setJwsSecret("test-jws-secret-at-least-32-chars-long");
		service = new PatientInvitationTokenServiceImpl(properties);
	}

	@Test
	void generate_returnsUrlTokenHumanCodeAndHash() {
		final PatientInvitationTokenBundle bundle = service.generate();

		assertThat(bundle.urlToken()).isNotBlank();
		assertThat(bundle.humanCode()).startsWith("NUTRI-");
		assertThat(bundle.tokenHash()).hasSize(64);
		assertThat(service.verify(bundle.urlToken(), bundle.tokenHash())).isTrue();
	}

	@Test
	void verify_rejectsWrongToken() {
		final PatientInvitationTokenBundle bundle = service.generate();

		assertThat(service.verify("not-the-token", bundle.tokenHash())).isFalse();
	}

	@Test
	void generate_humanCodeMatchesUrlTokenSecret() {
		final PatientInvitationTokenBundle bundle = service.generate();
		final byte[] secret = java.util.Base64.getUrlDecoder().decode(bundle.urlToken());

		assertThat(PatientInvitationHumanCode.matchesSecret(secret, "NUTRI", bundle.humanCode())).isTrue();
		assertThat(PatientInvitationHumanCode.fromUrlToken(bundle.urlToken(), "NUTRI")).isEqualTo(bundle.humanCode());
	}

	@Test
	void verify_rejectsBlankInputs() {
		final PatientInvitationTokenBundle bundle = service.generate();

		assertThat(service.verify("", bundle.tokenHash())).isFalse();
		assertThat(service.verify(bundle.urlToken(), "")).isFalse();
		assertThat(service.verify(null, bundle.tokenHash())).isFalse();
	}

	@Test
	void offlineJws_roundTripsPatientId() {
		final PatientInvitationTokenBundle bundle = service.generate();
		final Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

		final String jws = service.createOfflineJws(42L, bundle.urlToken(), expiresAt).orElseThrow();

		assertThat(service.verifyOfflineJws(jws)).contains(42L);
	}

	@Test
	void offlineJws_disabledWhenSecretMissing() {
		final PatientInvitationProperties properties = new PatientInvitationProperties();
		final PatientInvitationTokenService disabled = new PatientInvitationTokenServiceImpl(properties);
		final PatientInvitationTokenBundle bundle = disabled.generate();

		assertThat(disabled.createOfflineJws(1L, bundle.urlToken(), Instant.now().plus(1, ChronoUnit.DAYS))).isEmpty();
		assertThat(disabled.verifyOfflineJws("a.b.c")).isEmpty();
	}

	@Test
	void createOfflineJws_requiresPacienteId() {
		assertThatThrownBy(() -> service.createOfflineJws(null, "token", Instant.now().plus(1, ChronoUnit.DAYS)))
			.isInstanceOf(IllegalArgumentException.class);
	}

}
