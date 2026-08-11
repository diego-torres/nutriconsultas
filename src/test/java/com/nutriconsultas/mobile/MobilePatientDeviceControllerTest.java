package com.nutriconsultas.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import com.nutriconsultas.device.PatientDevicePlatform;
import com.nutriconsultas.mobile.dto.ApiResponse;
import com.nutriconsultas.mobile.dto.DeregisterPatientDeviceRequest;
import com.nutriconsultas.mobile.dto.PatientDeviceDto;
import com.nutriconsultas.mobile.dto.RegisterPatientDeviceRequest;
import com.nutriconsultas.paciente.projection.PacienteAuthView;

@ExtendWith(MockitoExtension.class)
class MobilePatientDeviceControllerTest {

	private static final String PATIENT_SUB = "auth0|mobile-device-patient";

	@InjectMocks
	private MobilePatientDeviceController controller;

	@Mock
	private PatientAuthService patientAuthService;

	@Mock
	private MobilePatientDeviceService mobilePatientDeviceService;

	@Test
	void registerDevice_returnsApiResponseEnvelope() {
		final PatientDeviceDto device = new PatientDeviceDto(12L, PatientDevicePlatform.IOS,
				Instant.parse("2026-08-11T16:00:00Z"));
		final Jwt jwt = jwtWithSub(PATIENT_SUB);
		final RegisterPatientDeviceRequest request = new RegisterPatientDeviceRequest(PatientDevicePlatform.IOS,
				"apns-token-abc", "1.2.3");

		when(patientAuthService.requireAuthViewByJwt(jwt)).thenReturn(authView(5L));
		when(mobilePatientDeviceService.upsertDevice(eq(5L), eq(PatientDevicePlatform.IOS), eq("apns-token-abc"),
				eq("1.2.3")))
			.thenReturn(device);

		final ApiResponse<PatientDeviceDto> response = controller.registerDevice(jwt, request);

		assertThat(response.data().id()).isEqualTo(12L);
		assertThat(response.data().platform()).isEqualTo(PatientDevicePlatform.IOS);
		assertThat(response.timestamp()).isNotNull();
		verify(mobilePatientDeviceService).upsertDevice(5L, PatientDevicePlatform.IOS, "apns-token-abc", "1.2.3");
	}

	@Test
	void deregisterDevice_delegatesToService() {
		final Jwt jwt = jwtWithSub(PATIENT_SUB);
		when(patientAuthService.requireAuthViewByJwt(jwt)).thenReturn(authView(5L));

		controller.deregisterDevice(jwt, new DeregisterPatientDeviceRequest("apns-token-abc"));

		verify(mobilePatientDeviceService).deregisterDevice(5L, "apns-token-abc");
	}

	private static Jwt jwtWithSub(final String subject) {
		return Jwt.withTokenValue("token").header("alg", "none").subject(subject).build();
	}

	private static PacienteAuthView authView(final Long id) {
		return MobileTestPacienteAuthViews.authView(id, PATIENT_SUB, "auth0|nutritionist-owner");
	}

}
