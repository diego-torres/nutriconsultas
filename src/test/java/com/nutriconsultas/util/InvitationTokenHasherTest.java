package com.nutriconsultas.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InvitationTokenHasherTest {

	@Test
	void generateTokenAndVerifyRoundTrip() {
		final String rawToken = InvitationTokenHasher.generateToken();
		final String hash = InvitationTokenHasher.hashToken(rawToken);

		assertThat(rawToken).isNotBlank();
		assertThat(hash).hasSize(64);
		assertThat(InvitationTokenHasher.verifyToken(rawToken, hash)).isTrue();
	}

	@Test
	void verifyTokenRejectsInvalidToken() {
		final String hash = InvitationTokenHasher.hashToken(InvitationTokenHasher.generateToken());

		assertThat(InvitationTokenHasher.verifyToken("wrong-token", hash)).isFalse();
		assertThat(InvitationTokenHasher.verifyToken(null, hash)).isFalse();
	}

}
