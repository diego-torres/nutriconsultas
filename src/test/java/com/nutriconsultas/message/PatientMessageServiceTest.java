package com.nutriconsultas.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.message.dto.PatientMessageThreadItemDto;
import com.nutriconsultas.message.dto.PatientUnreadMessageDto;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;

@ExtendWith(MockitoExtension.class)
class PatientMessageServiceTest {

	private static final String USER_ID = "auth0|nutritionist";

	@InjectMocks
	private PatientMessageService patientMessageService;

	@Mock
	private PatientMessageRepository patientMessageRepository;

	@Mock
	private PacienteRepository pacienteRepository;

	@Test
	void listThread_returnsAscendingMessages() {
		final Paciente paciente = samplePaciente(1L);
		final PatientMessage message = sampleMessage(paciente, MessageSenderRole.PATIENT, "Hola");
		when(pacienteRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(paciente));
		when(patientMessageRepository.findThreadAscending(1L, USER_ID)).thenReturn(List.of(message));

		final List<PatientMessageThreadItemDto> thread = patientMessageService.listThread(1L, USER_ID);

		assertThat(thread).hasSize(1);
		assertThat(thread.get(0).body()).isEqualTo("Hola");
	}

	@Test
	void listUnreadSummaries_groupsByPatient() {
		final Paciente paciente = samplePaciente(1L);
		final PatientMessage first = sampleMessage(paciente, MessageSenderRole.PATIENT, "Uno");
		first.setReadByNutritionist(false);
		final PatientMessage second = sampleMessage(paciente, MessageSenderRole.PATIENT, "Dos");
		second.setReadByNutritionist(false);
		when(patientMessageRepository.findUnreadFromPatientsByNutritionist(USER_ID)).thenReturn(List.of(first, second));

		final List<PatientUnreadMessageDto> summaries = patientMessageService.listUnreadSummaries(USER_ID);

		assertThat(summaries).hasSize(1);
		assertThat(summaries.get(0).pacienteId()).isEqualTo(1L);
		assertThat(summaries.get(0).unreadCount()).isEqualTo(2);
	}

	@Test
	void sendAsNutritionist_persistsMessage() {
		final Paciente paciente = samplePaciente(1L);
		when(pacienteRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(paciente));
		when(patientMessageRepository.save(any(PatientMessage.class))).thenAnswer(invocation -> {
			final PatientMessage saved = invocation.getArgument(0);
			saved.setId(99L);
			saved.setSentAt(Instant.parse("2026-06-01T12:00:00Z"));
			return saved;
		});

		final PatientMessageThreadItemDto sent = patientMessageService.sendAsNutritionist(1L, USER_ID, "Respuesta");

		assertThat(sent.id()).isEqualTo(99L);
		assertThat(sent.senderRole()).isEqualTo(MessageSenderRole.NUTRITIONIST);
		final ArgumentCaptor<PatientMessage> captor = ArgumentCaptor.forClass(PatientMessage.class);
		verify(patientMessageRepository).save(captor.capture());
		assertThat(captor.getValue().getBody()).isEqualTo("Respuesta");
		assertThat(captor.getValue().isReadByNutritionist()).isTrue();
		assertThat(captor.getValue().isReadByPatient()).isFalse();
	}

	@Test
	void sendAsNutritionist_whenBodyBlank_throwsBadRequest() {
		final Paciente paciente = samplePaciente(1L);
		when(pacienteRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(paciente));

		assertThatThrownBy(() -> patientMessageService.sendAsNutritionist(1L, USER_ID, "   "))
			.isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
			.isEqualTo(HttpStatus.BAD_REQUEST);

		verify(patientMessageRepository, never()).save(any(PatientMessage.class));
	}

	@Test
	void sendAsNutritionist_whenPatientNotOwned_throwsNotFound() {
		when(pacienteRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> patientMessageService.sendAsNutritionist(1L, USER_ID, "Respuesta"))
			.isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
			.isEqualTo(HttpStatus.NOT_FOUND);

		verify(patientMessageRepository, never()).save(any(PatientMessage.class));
	}

	@Test
	void markThreadReadByNutritionist_delegatesToRepository() {
		final Paciente paciente = samplePaciente(1L);
		when(pacienteRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(paciente));
		when(patientMessageRepository.markReadByNutritionist(1L, USER_ID)).thenReturn(2);

		patientMessageService.markThreadReadByNutritionist(1L, USER_ID);

		verify(patientMessageRepository).markReadByNutritionist(1L, USER_ID);
	}

	private static Paciente samplePaciente(final Long id) {
		final Paciente paciente = new Paciente();
		paciente.setId(id);
		paciente.setName("Ana López");
		paciente.setUserId(USER_ID);
		paciente.setDob(new java.util.Date());
		paciente.setGender("F");
		return paciente;
	}

	private static PatientMessage sampleMessage(final Paciente paciente, final MessageSenderRole role,
			final String body) {
		final PatientMessage message = new PatientMessage();
		message.setId(10L);
		message.setPaciente(paciente);
		message.setNutritionistUserId(USER_ID);
		message.setSenderRole(role);
		message.setBody(body);
		message.setSentAt(Instant.parse("2026-06-01T10:00:00Z"));
		return message;
	}

}
