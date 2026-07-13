package com.nutriconsultas.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;

import com.nutriconsultas.mobile.dto.ApiResponse;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacientePhotoService;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.PacienteStatus;

@ExtendWith(MockitoExtension.class)
class MobilePatientPhotoControllerTest {

	private static final String PATIENT_SUB = "auth0|photo-patient";

	@InjectMocks
	private MobilePatientPhotoController controller;

	@Mock
	private PatientAuthService patientAuthService;

	@Mock
	private PacientePhotoService pacientePhotoService;

	@Mock
	private PacienteRepository pacienteRepository;

	@Test
	void getPhoto_redirectsWhenNoCustomPhoto() {
		final Jwt jwt = jwtWithSub(PATIENT_SUB);
		final Paciente paciente = pacienteWithId(8L);
		when(patientAuthService.requireAuthViewByJwt(jwt)).thenReturn(
				MobileTestPacienteAuthViews.authView(8L, PATIENT_SUB, "nutritionist-sub", PacienteStatus.ACTIVE));
		when(pacienteRepository.findById(8L)).thenReturn(Optional.of(paciente));

		final ResponseEntity<byte[]> response = controller.getPhoto(jwt);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
	}

	@Test
	void uploadPhoto_delegatesToServiceAndReturnsUrl() {
		final Jwt jwt = jwtWithSub(PATIENT_SUB);
		final Paciente paciente = pacienteWithId(8L);
		paciente.setPhotoExtension("png");
		final MockMultipartFile file = new MockMultipartFile("photoFile", "photo.png", "image/png",
				new byte[] { 9, 8, 7 });
		when(patientAuthService.requireAuthViewByJwt(jwt)).thenReturn(
				MobileTestPacienteAuthViews.authView(8L, PATIENT_SUB, "nutritionist-sub", PacienteStatus.ONBOARDING));
		when(pacienteRepository.findById(8L)).thenReturn(Optional.of(paciente));

		final ApiResponse<MobilePatientPhotoController.MobilePatientPhotoResponse> response = controller
			.uploadPhoto(jwt, file);

		verify(pacientePhotoService).savePhotoForPatient(eq(8L), any(byte[].class), eq("png"));
		assertThat(response.data().photoUrl()).isEqualTo("/rest/mobile/patient/profile/photo");
		assertThat(response.data().photoExtension()).isEqualTo("png");
	}

	private static Jwt jwtWithSub(final String subject) {
		return Jwt.withTokenValue("token").header("alg", "none").subject(subject).build();
	}

	private static Paciente pacienteWithId(final Long id) {
		final Paciente paciente = new Paciente();
		paciente.setId(id);
		paciente.setGender("F");
		return paciente;
	}

}
