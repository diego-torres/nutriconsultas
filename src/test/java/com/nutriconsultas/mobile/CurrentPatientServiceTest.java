package com.nutriconsultas.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.PacienteStatus;
import com.nutriconsultas.paciente.projection.PacienteAuthView;

@ExtendWith(MockitoExtension.class)
class CurrentPatientServiceTest {

	private static final String LINKED_SUB = "auth0|linked-patient";

	@Mock
	private PacienteRepository pacienteRepository;

	@InjectMocks
	private CurrentPatientService currentPatientService;

	@Test
	void findByJwt_returnsCurrentPatientWhenLinked() {
		final PacienteAuthView authView = MobileTestPacienteAuthViews.authView(7L, LINKED_SUB, "nutritionist-sub",
				PacienteStatus.ACTIVE);
		when(pacienteRepository.findAuthViewByPatientAuthSub(LINKED_SUB)).thenReturn(Optional.of(authView));

		final Optional<CurrentPatient> result = currentPatientService.findByJwt(jwtWithSub(LINKED_SUB));

		assertThat(result).contains(new CurrentPatient(7L, LINKED_SUB, PacienteStatus.ACTIVE));
	}

	@Test
	void requireByJwt_throwsWhenSubIsUnlinked() {
		when(pacienteRepository.findAuthViewByPatientAuthSub("auth0|unknown")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> currentPatientService.requireByJwt(jwtWithSub("auth0|unknown")))
			.isInstanceOf(PatientNotLinkedException.class);
	}

	@Test
	void resolvePrincipal_includesStatus() {
		final PacienteAuthView authView = MobileTestPacienteAuthViews.authView(42L, LINKED_SUB, "nutritionist-sub",
				PacienteStatus.ONBOARDING);
		when(pacienteRepository.findAuthViewByPatientAuthSub(LINKED_SUB)).thenReturn(Optional.of(authView));

		final PatientPrincipal principal = currentPatientService.resolvePrincipal(jwtWithSub(LINKED_SUB));

		assertThat(principal.getPacienteId()).isEqualTo(42L);
		assertThat(principal.getPatientAuthSub()).isEqualTo(LINKED_SUB);
		assertThat(principal.getStatus()).isEqualTo(PacienteStatus.ONBOARDING);
	}

	private static Jwt jwtWithSub(final String sub) {
		return Jwt.withTokenValue("test-token")
			.header("alg", "none")
			.subject(sub)
			.claim("aud", "https://api.nutriconsultas.test/mobile")
			.build();
	}

}
