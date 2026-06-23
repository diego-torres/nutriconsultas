package com.nutriconsultas.mobile;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.nutriconsultas.mobile.MobilePatientAccessRules.Decision;
import com.nutriconsultas.paciente.PacienteStatus;
import com.nutriconsultas.paciente.projection.PacienteAuthView;

class MobilePatientAccessRulesTest {

	private static final String VISITS_PATH = "/rest/mobile/patient/visits";

	private static final String PROFILE_PATH = "/rest/mobile/patient/me";

	@Test
	void evaluatePatientApiAccess_unlinkedPatientOnDataEndpoint_requiresOnboarding() {
		final Decision decision = MobilePatientAccessRules.evaluatePatientApiAccess(Optional.empty(), VISITS_PATH);

		assertThat(decision).isEqualTo(Decision.ONBOARDING_REQUIRED);
	}

	@Test
	void evaluatePatientApiAccess_onboardingPatientOnProfilePath_allowsAccess() {
		final PacienteAuthView authView = MobileTestPacienteAuthViews.authView(1L, "auth0|onboarding", "owner",
				PacienteStatus.ONBOARDING);

		final Decision decision = MobilePatientAccessRules.evaluatePatientApiAccess(Optional.of(authView),
				PROFILE_PATH);

		assertThat(decision).isEqualTo(Decision.ALLOW);
	}

	@Test
	void evaluatePatientApiAccess_onboardingPatientOnDataEndpoint_requiresOnboarding() {
		final PacienteAuthView authView = MobileTestPacienteAuthViews.authView(1L, "auth0|onboarding", "owner",
				PacienteStatus.ONBOARDING);

		final Decision decision = MobilePatientAccessRules.evaluatePatientApiAccess(Optional.of(authView), VISITS_PATH);

		assertThat(decision).isEqualTo(Decision.ONBOARDING_REQUIRED);
	}

	@Test
	void evaluatePatientApiAccess_revokedPatient_requiresOnboarding() {
		final PacienteAuthView authView = MobileTestPacienteAuthViews.authView(1L, "auth0|revoked", "owner",
				PacienteStatus.REVOKED);

		final Decision decision = MobilePatientAccessRules.evaluatePatientApiAccess(Optional.of(authView), VISITS_PATH);

		assertThat(decision).isEqualTo(Decision.ONBOARDING_REQUIRED);
	}

	@Test
	void evaluatePatientApiAccess_activePatientOnDataEndpoint_allowsAccess() {
		final PacienteAuthView authView = MobileTestPacienteAuthViews.authView(1L, "auth0|active", "owner",
				PacienteStatus.ACTIVE);

		final Decision decision = MobilePatientAccessRules.evaluatePatientApiAccess(Optional.of(authView), VISITS_PATH);

		assertThat(decision).isEqualTo(Decision.ALLOW);
	}

}
