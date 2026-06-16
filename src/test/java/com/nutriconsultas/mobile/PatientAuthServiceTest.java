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
import com.nutriconsultas.paciente.projection.PacienteAuthView;

@ExtendWith(MockitoExtension.class)
class PatientAuthServiceTest {

	private static final String LINKED_SUB = "auth0|linked-patient";

	private static final String NUTRITIONIST_SUB = "nutritionist-sub";

	@Mock
	private PacienteRepository pacienteRepository;

	@InjectMocks
	private PatientAuthService patientAuthService;

	@Test
	void findAuthViewByJwt_returnsViewWhenPatientAuthSubMatches() {
		final PacienteAuthView authView = sampleAuthView(LINKED_SUB, 7L);
		when(pacienteRepository.findAuthViewByPatientAuthSub(LINKED_SUB)).thenReturn(Optional.of(authView));

		final Optional<PacienteAuthView> result = patientAuthService.findAuthViewByJwt(jwtWithSub(LINKED_SUB));

		assertThat(result).contains(authView);
	}

	@Test
	void requireAuthViewByJwt_throwsWhenSubIsUnlinked() {
		when(pacienteRepository.findAuthViewByPatientAuthSub("auth0|unknown")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> patientAuthService.requireAuthViewByJwt(jwtWithSub("auth0|unknown")))
			.isInstanceOf(PatientNotLinkedException.class);
	}

	@Test
	void resolvePrincipal_returnsNonPhiIdentifiers() {
		final PacienteAuthView authView = sampleAuthView(LINKED_SUB, 42L);
		when(pacienteRepository.findAuthViewByPatientAuthSub(LINKED_SUB)).thenReturn(Optional.of(authView));

		final PatientPrincipal principal = patientAuthService.resolvePrincipal(jwtWithSub(LINKED_SUB));

		assertThat(principal.getPacienteId()).isEqualTo(42L);
		assertThat(principal.getPatientAuthSub()).isEqualTo(LINKED_SUB);
	}

	private static Jwt jwtWithSub(final String sub) {
		return Jwt.withTokenValue("test-token")
			.header("alg", "none")
			.subject(sub)
			.claim("aud", "https://api.nutriconsultas.test/mobile")
			.build();
	}

	private static PacienteAuthView sampleAuthView(final String patientAuthSub, final Long id) {
		return new PacienteAuthView() {
			@Override
			public Long getId() {
				return id;
			}

			@Override
			public String getPatientAuthSub() {
				return patientAuthSub;
			}

			@Override
			public String getUserId() {
				return NUTRITIONIST_SUB;
			}
		};
	}

}
