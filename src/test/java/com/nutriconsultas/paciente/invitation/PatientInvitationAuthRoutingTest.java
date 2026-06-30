package com.nutriconsultas.paciente.invitation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteStatus;

class PatientInvitationAuthRoutingTest {

	@Test
	void resolveAuthPath_withInvitedUnlinkedPatient_returnsCreateAccount() {
		assertThat(PatientInvitationAuthRouting.resolveAuthPath(PacienteStatus.INVITED, false))
			.isEqualTo(InvitationAuthPath.CREATE_ACCOUNT);
	}

	@Test
	void resolveAuthPath_withLinkedOrOnboardingOrActive_returnsSignIn() {
		assertThat(PatientInvitationAuthRouting.resolveAuthPath(PacienteStatus.INVITED, true))
			.isEqualTo(InvitationAuthPath.SIGN_IN);
		assertThat(PatientInvitationAuthRouting.resolveAuthPath(PacienteStatus.ONBOARDING, true))
			.isEqualTo(InvitationAuthPath.SIGN_IN);
		assertThat(PatientInvitationAuthRouting.resolveAuthPath(PacienteStatus.ACTIVE, true))
			.isEqualTo(InvitationAuthPath.SIGN_IN);
		assertThat(PatientInvitationAuthRouting.resolveAuthPath(PacienteStatus.REVOKED, false))
			.isEqualTo(InvitationAuthPath.SIGN_IN);
	}

	@Test
	void isMobileAppLinked_reflectsPatientAuthSubPresence() {
		final Paciente linked = new Paciente();
		linked.setPatientAuthSub("auth0|patient");
		assertThat(PatientInvitationAuthRouting.isMobileAppLinked(linked)).isTrue();

		final Paciente unlinked = new Paciente();
		assertThat(PatientInvitationAuthRouting.isMobileAppLinked(unlinked)).isFalse();
	}

}
