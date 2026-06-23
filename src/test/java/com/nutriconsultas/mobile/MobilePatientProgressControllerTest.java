package com.nutriconsultas.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import com.nutriconsultas.mobile.dto.ApiResponse;
import com.nutriconsultas.mobile.dto.PatientProgressSnapshotDto;
import com.nutriconsultas.mobile.dto.ProgressMeasurementPointDto;
import com.nutriconsultas.mobile.dto.ProgressMeasurementsDto;
import com.nutriconsultas.paciente.NivelPeso;
import com.nutriconsultas.paciente.projection.PacienteAuthView;

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
		final PatientProgressSnapshotDto snapshot = new PatientProgressSnapshotDto(
				Instant.parse("2026-06-01T10:00:00Z"), null, 70.0, 1.70, 24.2, NivelPeso.NORMAL, "Normal", 1500.0, 22.0,
				null, null, null, "avatar_1", "/sbadmin/img/paciente-avatars/avatar_1.png");
		final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(PATIENT_SUB).build();

		when(patientAuthService.requireAuthViewByJwt(jwt)).thenReturn(authView(5L));
		when(mobilePatientProgressService.getSnapshot(5L)).thenReturn(snapshot);

		final ApiResponse<PatientProgressSnapshotDto> response = controller.getProgress(jwt);

		assertThat(response.data().bmi()).isEqualTo(24.2);
		assertThat(response.data().imcLabel()).isEqualTo("Normal");
		assertThat(response.timestamp()).isNotNull();
		verify(mobilePatientProgressService).getSnapshot(5L);
	}

	@Test
	void listMeasurements_returnsApiResponseEnvelope() {
		final ProgressMeasurementsDto series = new ProgressMeasurementsDto(List
			.of(new ProgressMeasurementPointDto(Instant.parse("2026-06-01T10:00:00Z"), 70.0, 1.70, 24.2, 22.0, null)),
				1, false);
		final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(PATIENT_SUB).build();

		when(patientAuthService.requireAuthViewByJwt(jwt)).thenReturn(authView(5L));
		when(mobilePatientProgressService.listMeasurements(5L, null, null, null)).thenReturn(series);

		final ApiResponse<ProgressMeasurementsDto> response = controller.listMeasurements(jwt, null, null, null);

		assertThat(response.data().count()).isEqualTo(1);
		assertThat(response.data().measurements()).hasSize(1);
		assertThat(response.data().measurements().get(0).weightKg()).isEqualTo(70.0);
		assertThat(response.timestamp()).isNotNull();
		verify(mobilePatientProgressService).listMeasurements(5L, null, null, null);
	}

	private static PacienteAuthView authView(final Long id) {
		return MobileTestPacienteAuthViews.authView(id, PATIENT_SUB, "auth0|nutritionist");
	}

}
