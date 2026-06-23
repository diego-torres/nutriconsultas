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

import com.nutriconsultas.paciente.PacienteStatus;
import com.nutriconsultas.paciente.projection.PacienteAuthView;

@ExtendWith(MockitoExtension.class)
class PatientAuthServiceTest {

	private static final String LINKED_SUB = "auth0|linked-patient";

	@Mock
	private CurrentPatientService currentPatientService;

	@InjectMocks
	private PatientAuthService patientAuthService;

	@Test
	void findAuthViewByJwt_returnsViewWhenPatientAuthSubMatches() {
		final PacienteAuthView authView = MobileTestPacienteAuthViews.authView(7L, LINKED_SUB, "nutritionist-sub");
		when(currentPatientService.findAuthViewByJwt(jwtWithSub(LINKED_SUB))).thenReturn(Optional.of(authView));

		final Optional<PacienteAuthView> result = patientAuthService.findAuthViewByJwt(jwtWithSub(LINKED_SUB));

		assertThat(result).contains(authView);
	}

	@Test
	void requireAuthViewByJwt_throwsWhenSubIsUnlinked() {
		when(currentPatientService.findAuthViewByJwt(jwtWithSub("auth0|unknown"))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> patientAuthService.requireAuthViewByJwt(jwtWithSub("auth0|unknown")))
			.isInstanceOf(PatientNotLinkedException.class);
	}

	@Test
	void resolvePrincipal_delegatesToCurrentPatientService() {
		final PatientPrincipal principal = new PatientPrincipal(42L, LINKED_SUB, PacienteStatus.ACTIVE);
		when(currentPatientService.resolvePrincipal(jwtWithSub(LINKED_SUB))).thenReturn(principal);

		final PatientPrincipal result = patientAuthService.resolvePrincipal(jwtWithSub(LINKED_SUB));

		assertThat(result).isEqualTo(principal);
	}

	private static Jwt jwtWithSub(final String sub) {
		return Jwt.withTokenValue("test-token")
			.header("alg", "none")
			.subject(sub)
			.claim("aud", "https://api.nutriconsultas.test/mobile")
			.build();
	}

}
