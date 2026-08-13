package com.nutriconsultas.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import com.nutriconsultas.mobile.dto.AppointmentQuestionDto;
import com.nutriconsultas.mobile.dto.CreateAppointmentQuestionRequest;
import com.nutriconsultas.mobile.dto.PagedResponse;
import com.nutriconsultas.mobile.dto.PatchAppointmentQuestionRequest;
import com.nutriconsultas.paciente.projection.PacienteAuthView;

@ExtendWith(MockitoExtension.class)
class MobilePatientAppointmentQuestionControllerTest {

	private static final String PATIENT_SUB = "auth0|mobile-appt-q-patient";

	@InjectMocks
	private MobilePatientAppointmentQuestionController controller;

	@Mock
	private PatientAuthService patientAuthService;

	@Mock
	private MobilePatientAppointmentQuestionService mobilePatientAppointmentQuestionService;

	@Test
	void listQuestions_returnsApiResponseEnvelope() {
		final AppointmentQuestionDto question = sampleDto(1L);
		final PagedResponse<AppointmentQuestionDto> page = new PagedResponse<>(List.of(question), 0, 20, 1, 1, true);
		final Jwt jwt = jwtWithSub(PATIENT_SUB);
		when(patientAuthService.requireAuthViewByJwt(jwt)).thenReturn(authView(5L));
		when(mobilePatientAppointmentQuestionService.listQuestions(5L, 0, 20, null)).thenReturn(page);

		final ApiResponse<PagedResponse<AppointmentQuestionDto>> response = controller.listQuestions(jwt, 0, 20, null);

		assertThat(response.data().content()).hasSize(1);
		assertThat(response.data().content().get(0).id()).isEqualTo(1L);
		assertThat(response.timestamp()).isNotNull();
	}

	@Test
	void createQuestion_delegatesToService() {
		final Jwt jwt = jwtWithSub(PATIENT_SUB);
		final PacienteAuthView authView = authView(5L);
		final AppointmentQuestionDto created = sampleDto(9L);
		when(patientAuthService.requireAuthViewByJwt(jwt)).thenReturn(authView);
		when(mobilePatientAppointmentQuestionService.createQuestion(eq(authView), eq("¿Puedo comer pan?")))
			.thenReturn(created);

		final ApiResponse<AppointmentQuestionDto> response = controller.createQuestion(jwt,
				new CreateAppointmentQuestionRequest("¿Puedo comer pan?"));

		assertThat(response.data().id()).isEqualTo(9L);
		verify(mobilePatientAppointmentQuestionService).createQuestion(authView, "¿Puedo comer pan?");
	}

	@Test
	void patchQuestion_delegatesToService() {
		final Jwt jwt = jwtWithSub(PATIENT_SUB);
		when(patientAuthService.requireAuthViewByJwt(jwt)).thenReturn(authView(5L));
		when(mobilePatientAppointmentQuestionService.patchQuestion(eq(5L), eq(3L), isNull(), eq(true)))
			.thenReturn(sampleDto(3L));

		controller.patchQuestion(jwt, 3L, new PatchAppointmentQuestionRequest(null, true));

		verify(mobilePatientAppointmentQuestionService).patchQuestion(5L, 3L, null, true);
	}

	@Test
	void deleteQuestion_delegatesToService() {
		final Jwt jwt = jwtWithSub(PATIENT_SUB);
		when(patientAuthService.requireAuthViewByJwt(jwt)).thenReturn(authView(5L));

		controller.deleteQuestion(jwt, 3L);

		verify(mobilePatientAppointmentQuestionService).deleteQuestion(5L, 3L);
	}

	private static AppointmentQuestionDto sampleDto(final Long id) {
		return new AppointmentQuestionDto(id, "¿Puedo comer mango?", false, null, Instant.parse("2026-08-13T12:00:00Z"),
				Instant.parse("2026-08-13T12:00:00Z"));
	}

	private static Jwt jwtWithSub(final String subject) {
		return Jwt.withTokenValue("token").header("alg", "none").subject(subject).build();
	}

	private static PacienteAuthView authView(final Long id) {
		return MobileTestPacienteAuthViews.authView(id, PATIENT_SUB, "auth0|nutritionist-owner");
	}

}
