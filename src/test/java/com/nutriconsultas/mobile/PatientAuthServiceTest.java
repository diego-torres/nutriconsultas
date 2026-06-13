package com.nutriconsultas.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;

@ExtendWith(MockitoExtension.class)
class PatientAuthServiceTest {

	private static final String LINKED_SUB = "auth0|linked-patient";

	@Mock
	private PacienteRepository pacienteRepository;

	@InjectMocks
	private PatientAuthService patientAuthService;

	@Test
	void findByJwt_returnsPacienteWhenPatientAuthSubMatches() {
		final Paciente paciente = samplePaciente(LINKED_SUB);
		when(pacienteRepository.findByPatientAuthSub(LINKED_SUB)).thenReturn(Optional.of(paciente));

		final Optional<Paciente> result = patientAuthService.findByJwt(jwtWithSub(LINKED_SUB));

		assertThat(result).contains(paciente);
	}

	@Test
	void requirePacienteByJwt_throwsWhenSubIsUnlinked() {
		when(pacienteRepository.findByPatientAuthSub("auth0|unknown")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> patientAuthService.requirePacienteByJwt(jwtWithSub("auth0|unknown")))
			.isInstanceOf(PatientNotLinkedException.class);
	}

	@Test
	void resolvePrincipal_returnsNonPhiIdentifiers() {
		final Paciente paciente = samplePaciente(LINKED_SUB);
		paciente.setId(42L);
		when(pacienteRepository.findByPatientAuthSub(LINKED_SUB)).thenReturn(Optional.of(paciente));

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

	private static Paciente samplePaciente(final String patientAuthSub) {
		final Paciente paciente = new Paciente();
		paciente.setName("Test Patient");
		paciente.setUserId("nutritionist-sub");
		paciente.setPatientAuthSub(patientAuthSub);
		final LocalDate dob = LocalDate.now().minusYears(30);
		paciente.setDob(Date.from(dob.atStartOfDay(ZoneId.systemDefault()).toInstant()));
		paciente.setGender("M");
		return paciente;
	}

}
