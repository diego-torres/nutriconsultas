package com.nutriconsultas.paciente.invitation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.nutriconsultas.util.InvitationTokenHasher;

class PatientInvitationUrlTokensTest {

	@Test
	void isWellFormed_acceptsGeneratedToken() {
		final String token = InvitationTokenHasher.generateToken();
		assertThat(PatientInvitationUrlTokens.isWellFormed(token)).isTrue();
	}

	@Test
	void isWellFormed_rejectsBlankOrWrongLength() {
		assertThat(PatientInvitationUrlTokens.isWellFormed(null)).isFalse();
		assertThat(PatientInvitationUrlTokens.isWellFormed("")).isFalse();
		assertThat(PatientInvitationUrlTokens.isWellFormed("short")).isFalse();
	}

	@Test
	void isWellFormed_rejectsInvalidCharacters() {
		final String token = InvitationTokenHasher.generateToken();
		assertThat(PatientInvitationUrlTokens.isWellFormed(token + "!")).isFalse();
	}

}
