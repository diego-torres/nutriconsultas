package com.nutriconsultas.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import com.nutriconsultas.mobile.dto.ApiResponse;
import com.nutriconsultas.mobile.dto.PatchPatientOnboardingProfileRequest;
import com.nutriconsultas.mobile.dto.PatientOnboardingProfileDto;
import com.nutriconsultas.paciente.PacienteAvatarCatalog;
import com.nutriconsultas.paciente.PacienteStatus;
import com.nutriconsultas.paciente.projection.PacienteAuthView;

@ExtendWith(MockitoExtension.class)
class MobilePatientOnboardingControllerTest {

	private static final String PATIENT_SUB = "auth0|onboarding-patient";

	@InjectMocks
	private MobilePatientOnboardingController controller;

	@Mock
	private PatientAuthService patientAuthService;

	@Mock
	private MobilePatientOnboardingService mobilePatientOnboardingService;

	@Test
	void getProfile_returnsEnvelope() {
		final Jwt jwt = jwtWithSub(PATIENT_SUB);
		when(patientAuthService.requireAuthViewByJwt(jwt)).thenReturn(
				MobileTestPacienteAuthViews.authView(12L, PATIENT_SUB, "nutritionist-sub", PacienteStatus.ONBOARDING));
		final PatientOnboardingProfileDto profile = new PatientOnboardingProfileDto(12L, PacienteStatus.ONBOARDING,
				"María López", "María", LocalDate.of(1990, 5, 15), "F", "maria@example.com", null, null, "P-001", false,
				"Lic. Ana López", null);
		when(mobilePatientOnboardingService.getProfile(12L)).thenReturn(profile);

		final ApiResponse<PatientOnboardingProfileDto> response = controller.getProfile(jwt);

		assertThat(response.data()).isEqualTo(profile);
		verify(mobilePatientOnboardingService).getProfile(12L);
	}

	@Test
	void updateProfile_delegatesToService() {
		final Jwt jwt = jwtWithSub(PATIENT_SUB);
		when(patientAuthService.requireAuthViewByJwt(jwt)).thenReturn(
				MobileTestPacienteAuthViews.authView(12L, PATIENT_SUB, "nutritionist-sub", PacienteStatus.ONBOARDING));
		final PatchPatientOnboardingProfileRequest request = new PatchPatientOnboardingProfileRequest(null, null, null,
				null, null, null, PacienteAvatarCatalog.DEFAULT_FEMALE_ID);
		final PatientOnboardingProfileDto profile = new PatientOnboardingProfileDto(12L, PacienteStatus.ACTIVE,
				"María López", "María", LocalDate.of(1990, 5, 15), "F", "maria@example.com", null,
				PacienteAvatarCatalog.DEFAULT_FEMALE_ID, "P-001", true, "Lic. Ana López", null);
		when(mobilePatientOnboardingService.updateProfile(12L, request)).thenReturn(profile);

		final ApiResponse<PatientOnboardingProfileDto> response = controller.updateProfile(jwt, request);

		assertThat(response.data().status()).isEqualTo(PacienteStatus.ACTIVE);
		verify(mobilePatientOnboardingService).updateProfile(12L, request);
	}

	private static Jwt jwtWithSub(final String subject) {
		return Jwt.withTokenValue("token").header("alg", "none").subject(subject).build();
	}

}
