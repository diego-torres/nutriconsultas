package com.nutriconsultas.paciente.invitation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PatientInvitationHumanCodesTest {

	@Test
	void isWellFormed_acceptsValidGeneratedCode() {
		final PatientInvitationTokenService service = new PatientInvitationTokenServiceImpl(
				new PatientInvitationProperties());
		final PatientInvitationTokenBundle bundle = service.generate();

		assertThat(PatientInvitationHumanCodes.isWellFormed(bundle.humanCode(), "NUTRI")).isTrue();
		assertThat(PatientInvitationHumanCodes.normalize("  nutri-abcd-efgh  ")).isEqualTo("NUTRI-ABCD-EFGH");
	}

	@Test
	void isWellFormed_rejectsMalformedOrChecksumMismatch() {
		assertThat(PatientInvitationHumanCodes.isWellFormed(null, "NUTRI")).isFalse();
		assertThat(PatientInvitationHumanCodes.isWellFormed("", "NUTRI")).isFalse();
		assertThat(PatientInvitationHumanCodes.isWellFormed("NUTRI-ABCD", "NUTRI")).isFalse();
		assertThat(PatientInvitationHumanCodes.isWellFormed("NUTRI-ABCD-EFG1", "NUTRI")).isFalse();
		assertThat(PatientInvitationHumanCodes.isWellFormed("WRONG-ABCD-EFGH", "NUTRI")).isFalse();
	}

}
