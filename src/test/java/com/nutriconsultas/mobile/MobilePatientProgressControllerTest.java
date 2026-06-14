package com.nutriconsultas.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import com.nutriconsultas.mobile.dto.ApiResponse;
import com.nutriconsultas.mobile.dto.PatientProgressSnapshotDto;
import com.nutriconsultas.paciente.NivelPeso;
import com.nutriconsultas.paciente.Paciente;

@ExtendWith(MockitoExtension.class)
class MobilePatientProgressControllerTest {

	private static final String PATIENT_SUB = "auth0|mobile-progress-patient";

	@InjectMocks
	private MobilePatientProgressController controller;

	@Mock
	private PatientAuthService patientAuthService;

	@Mock
	private MobilePatientProgressService mobilePatientProgressService;

	@Test
	void getProgress_returnsApiResponseEnvelope() {
		final Paciente paciente = new Paciente();
		paciente.setId(5L);
		final PatientProgressSnapshotDto snapshot = new PatientProgressSnapshotDto(
				Instant.parse("2026-06-01T10:00:00Z"), null, 70.0, 1.70, 24.2, NivelPeso.NORMAL, "Normal", 1500.0, 22.0,
				null, null, null);
		final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(PATIENT_SUB).build();

		when(patientAuthService.requirePacienteByJwt(jwt)).thenReturn(paciente);
		when(mobilePatientProgressService.getSnapshot(5L)).thenReturn(snapshot);

		final ApiResponse<PatientProgressSnapshotDto> response = controller.getProgress(jwt);

		assertThat(response.data().bmi()).isEqualTo(24.2);
		assertThat(response.data().imcLabel()).isEqualTo("Normal");
		assertThat(response.timestamp()).isNotNull();
		verify(mobilePatientProgressService).getSnapshot(5L);
	}

}
