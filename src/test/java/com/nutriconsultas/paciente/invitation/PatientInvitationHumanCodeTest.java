package com.nutriconsultas.paciente.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import org.junit.jupiter.api.Test;

class PatientInvitationHumanCodeTest {

	@Test
	void format_producesPrefixedGroupsWithChecksum() {
		final byte[] secret = new byte[32];
		for (int index = 0; index < secret.length; index++) {
			secret[index] = (byte) index;
		}

		final String code = PatientInvitationHumanCode.format(secret, "NUTRI");

		assertThat(code).startsWith("NUTRI-");
		assertThat(code.split("-")).hasSize(3);
		assertThat(code.split("-")[1]).hasSize(4);
		assertThat(code.split("-")[2]).hasSize(4);
	}

	@Test
	void matchesSecret_validatesFormattedCode() {
		final byte[] secret = Base64.getUrlDecoder().decode("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
		final String code = PatientInvitationHumanCode.format(secret, "NUTRI");

		assertThat(PatientInvitationHumanCode.matchesSecret(secret, "NUTRI", code)).isTrue();
		assertThat(PatientInvitationHumanCode.matchesSecret(secret, "NUTRI", code.substring(0, code.length() - 1) + "Z"))
			.isFalse();
	}

	@Test
	void format_rejectsShortSecret() {
		assertThatThrownBy(() -> PatientInvitationHumanCode.format(new byte[2], "NUTRI"))
			.isInstanceOf(IllegalArgumentException.class);
	}

}
