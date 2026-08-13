package com.nutriconsultas.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.appointmentquestion.AppointmentQuestion;
import com.nutriconsultas.appointmentquestion.AppointmentQuestionRepository;
import com.nutriconsultas.mobile.dto.AppointmentQuestionDto;
import com.nutriconsultas.mobile.dto.PagedResponse;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.projection.PacienteAuthView;

@ExtendWith(MockitoExtension.class)
class MobilePatientAppointmentQuestionServiceTest {

	@InjectMocks
	private MobilePatientAppointmentQuestionService service;

	@Mock
	private AppointmentQuestionRepository appointmentQuestionRepository;

	@Mock
	private PacienteRepository pacienteRepository;

	@Mock
	private PatientWriteRateLimiter patientWriteRateLimiter;

	@Test
	void listQuestions_returnsPagedResponse() {
		final AppointmentQuestion question = sampleQuestion(10L, "¿Puedo comer mango?", false);
		when(appointmentQuestionRepository.findByPacienteIdOrderByCreatedAtDesc(eq(5L), any()))
			.thenReturn(new PageImpl<>(List.of(question), PageRequest.of(0, 20), 1));

		final PagedResponse<AppointmentQuestionDto> page = service.listQuestions(5L, 0, 20, null);

		assertThat(page.content()).hasSize(1);
		assertThat(page.content().get(0).id()).isEqualTo(10L);
		assertThat(page.content().get(0).body()).isEqualTo("¿Puedo comer mango?");
		assertThat(page.totalElements()).isEqualTo(1);
		assertThat(page.last()).isTrue();
	}

	@Test
	void listQuestions_filtersByAnswered() {
		when(appointmentQuestionRepository.findByPacienteIdAndAnsweredOrderByCreatedAtDesc(eq(5L), eq(true), any()))
			.thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

		final PagedResponse<AppointmentQuestionDto> page = service.listQuestions(5L, 0, 20, true);

		assertThat(page.content()).isEmpty();
		verify(appointmentQuestionRepository).findByPacienteIdAndAnsweredOrderByCreatedAtDesc(eq(5L), eq(true), any());
	}

	@Test
	void getQuestion_throwsNotFoundForOtherPatient() {
		when(appointmentQuestionRepository.findByIdAndPacienteId(99L, 5L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getQuestion(5L, 99L)).isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
			.isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void createQuestion_persistsTrimmedBody() {
		final PacienteAuthView authView = MobileTestPacienteAuthViews.authView(5L, "auth0|patient",
				"auth0|nutritionist");
		when(patientWriteRateLimiter.execute(eq(PatientWriteRateLimiter.PATIENT_APPOINTMENT_QUESTIONS),
				eq("auth0|patient"), any()))
			.thenAnswer(invocation -> {
				@SuppressWarnings("unchecked")
				final Callable<AppointmentQuestionDto> callable = invocation.getArgument(2);
				return callable.call();
			});
		when(pacienteRepository.getReferenceById(5L)).thenReturn(paciente(5L));
		when(appointmentQuestionRepository.save(any(AppointmentQuestion.class))).thenAnswer(invocation -> {
			final AppointmentQuestion question = invocation.getArgument(0);
			question.setId(42L);
			question.setCreatedAt(Instant.parse("2026-08-13T12:00:00Z"));
			question.setUpdatedAt(Instant.parse("2026-08-13T12:00:00Z"));
			return question;
		});

		final AppointmentQuestionDto dto = service.createQuestion(authView, "  ¿Debo tomar más agua?  ");

		assertThat(dto.id()).isEqualTo(42L);
		assertThat(dto.body()).isEqualTo("¿Debo tomar más agua?");
		assertThat(dto.answered()).isFalse();
		final ArgumentCaptor<AppointmentQuestion> captor = ArgumentCaptor.forClass(AppointmentQuestion.class);
		verify(appointmentQuestionRepository).save(captor.capture());
		assertThat(captor.getValue().getPaciente().getId()).isEqualTo(5L);
	}

	@Test
	void patchQuestion_marksAnsweredAndSetsAnsweredAt() {
		final AppointmentQuestion question = sampleQuestion(10L, "Pregunta", false);
		when(appointmentQuestionRepository.findByIdAndPacienteId(10L, 5L)).thenReturn(Optional.of(question));
		when(appointmentQuestionRepository.save(any(AppointmentQuestion.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		final AppointmentQuestionDto dto = service.patchQuestion(5L, 10L, null, true);

		assertThat(dto.answered()).isTrue();
		assertThat(dto.answeredAt()).isNotNull();
		assertThat(question.isAnswered()).isTrue();
	}

	@Test
	void patchQuestion_rejectsBlankBody() {
		final AppointmentQuestion question = sampleQuestion(10L, "Pregunta", false);
		when(appointmentQuestionRepository.findByIdAndPacienteId(10L, 5L)).thenReturn(Optional.of(question));

		assertThatThrownBy(() -> service.patchQuestion(5L, 10L, "   ", null))
			.isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
			.isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void deleteQuestion_removesOwnedRow() {
		final AppointmentQuestion question = sampleQuestion(10L, "Pregunta", false);
		when(appointmentQuestionRepository.findByIdAndPacienteId(10L, 5L)).thenReturn(Optional.of(question));

		service.deleteQuestion(5L, 10L);

		verify(appointmentQuestionRepository).delete(question);
	}

	private static AppointmentQuestion sampleQuestion(final Long id, final String body, final boolean answered) {
		final AppointmentQuestion question = new AppointmentQuestion();
		question.setId(id);
		question.setPaciente(paciente(5L));
		question.setBody(body);
		question.setAnswered(answered);
		question.setCreatedAt(Instant.parse("2026-08-13T10:00:00Z"));
		question.setUpdatedAt(Instant.parse("2026-08-13T10:00:00Z"));
		return question;
	}

	private static Paciente paciente(final Long id) {
		final Paciente paciente = new Paciente();
		paciente.setId(id);
		return paciente;
	}

}
