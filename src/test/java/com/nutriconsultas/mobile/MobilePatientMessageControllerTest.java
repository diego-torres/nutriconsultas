package com.nutriconsultas.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
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

import com.nutriconsultas.message.MessageSenderRole;
import com.nutriconsultas.mobile.dto.ApiResponse;
import com.nutriconsultas.mobile.dto.CursorPagedResponse;
import com.nutriconsultas.mobile.dto.PatientMessageSummaryDto;
import com.nutriconsultas.mobile.dto.SendPatientMessageRequest;
import com.nutriconsultas.paciente.projection.PacienteAuthView;

@ExtendWith(MockitoExtension.class)
class MobilePatientMessageControllerTest {

	private static final String PATIENT_SUB = "auth0|mobile-message-patient";

	@InjectMocks
	private MobilePatientMessageController controller;

	@Mock
	private PatientAuthService patientAuthService;

	@Mock
	private MobilePatientMessageService mobilePatientMessageService;

	@Test
	void listMessages_returnsApiResponseEnvelope() {
		final PatientMessageSummaryDto summary = new PatientMessageSummaryDto(1L, Instant.parse("2026-06-01T12:00:00Z"),
				MessageSenderRole.PATIENT, "Hola", true, null);
		final CursorPagedResponse<PatientMessageSummaryDto> page = CursorPagedResponse.of(List.of(summary), null);
		final Jwt jwt = jwtWithSub(PATIENT_SUB);

		when(patientAuthService.requireAuthViewByJwt(jwt)).thenReturn(authView(5L));
		when(mobilePatientMessageService.listMessages(eq(5L), eq(null), eq(20))).thenReturn(page);

		final ApiResponse<CursorPagedResponse<PatientMessageSummaryDto>> response = controller.listMessages(jwt, null,
				20);

		assertThat(response.data().content()).hasSize(1);
		assertThat(response.data().content().get(0).body()).isEqualTo("Hola");
		assertThat(response.timestamp()).isNotNull();
		verify(mobilePatientMessageService).listMessages(5L, null, 20);
	}

	@Test
	void sendMessage_returnsCreatedMessageInApiResponseEnvelope() {
		final PacienteAuthView authView = authView(5L);
		final PatientMessageSummaryDto sent = new PatientMessageSummaryDto(9L, Instant.parse("2026-06-01T12:00:00Z"),
				MessageSenderRole.PATIENT, "Hola doctor", true, null);
		final Jwt jwt = jwtWithSub(PATIENT_SUB);

		when(patientAuthService.requireAuthViewByJwt(jwt)).thenReturn(authView);
		when(mobilePatientMessageService.sendMessage(authView, "Hola doctor")).thenReturn(sent);

		final ApiResponse<PatientMessageSummaryDto> response = controller.sendMessage(jwt,
				new SendPatientMessageRequest("Hola doctor"));

		assertThat(response.data().id()).isEqualTo(9L);
		assertThat(response.data().senderRole()).isEqualTo(MessageSenderRole.PATIENT);
		assertThat(response.data().body()).isEqualTo("Hola doctor");
		verify(mobilePatientMessageService).sendMessage(authView, "Hola doctor");
	}

	private static Jwt jwtWithSub(final String subject) {
		return Jwt.withTokenValue("token").header("alg", "none").subject(subject).build();
	}

	private static PacienteAuthView authView(final Long id) {
		return MobileTestPacienteAuthViews.authView(id, PATIENT_SUB, "auth0|nutritionist-owner");
	}

}
