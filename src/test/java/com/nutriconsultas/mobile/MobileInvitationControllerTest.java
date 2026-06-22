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
import com.nutriconsultas.mobile.dto.CreatePatientInvitationRequest;
import com.nutriconsultas.mobile.dto.CreatedPatientInvitationDto;
import com.nutriconsultas.paciente.invitation.CreatedPatientInvitationResult;
import com.nutriconsultas.paciente.invitation.PatientInvitationCreateService;

@ExtendWith(MockitoExtension.class)
class MobileInvitationControllerTest {

	private static final String NUTRITIONIST_SUB = "auth0|nutritionist-mobile-invite";

	@InjectMocks
	private MobileInvitationController controller;

	@Mock
	private PatientInvitationCreateService patientInvitationCreateService;

	@Test
	void createInvitation_returnsApiResponseEnvelope() {
		final CreatePatientInvitationRequest request = new CreatePatientInvitationRequest("Juan Pérez",
				"juan@example.com", java.time.LocalDate.of(1988, 1, 2), "M", null, null, null);
		final CreatedPatientInvitationResult result = new CreatedPatientInvitationResult(10L, 20L,
				"https://links.test/i/token", "NUTRI-WXYZ-1234", Instant.parse("2026-07-01T00:00:00Z"), null);
		final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(NUTRITIONIST_SUB).build();

		when(patientInvitationCreateService.createInvitation(NUTRITIONIST_SUB, request)).thenReturn(result);

		final ApiResponse<CreatedPatientInvitationDto> response = controller.createInvitation(jwt, request);

		assertThat(response.data().invitationId()).isEqualTo(10L);
		assertThat(response.data().pacienteId()).isEqualTo(20L);
		assertThat(response.data().humanCode()).isEqualTo("NUTRI-WXYZ-1234");
		assertThat(response.timestamp()).isNotNull();
		verify(patientInvitationCreateService).createInvitation(NUTRITIONIST_SUB, request);
	}

}
