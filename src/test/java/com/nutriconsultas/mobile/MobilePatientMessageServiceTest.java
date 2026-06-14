package com.nutriconsultas.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.nutriconsultas.message.MessageSenderRole;
import com.nutriconsultas.message.PatientMessage;
import com.nutriconsultas.message.PatientMessageRepository;
import com.nutriconsultas.mobile.dto.CursorPagedResponse;
import com.nutriconsultas.mobile.dto.PatientMessageSummaryDto;
import com.nutriconsultas.paciente.Paciente;

@ExtendWith(MockitoExtension.class)
class MobilePatientMessageServiceTest {

	@InjectMocks
	private MobilePatientMessageService service;

	@Mock
	private PatientMessageRepository patientMessageRepository;

	@Test
	void listMessages_returnsCursorPageWithoutNextWhenNoMore() {
		final PatientMessage message = sampleMessage(42L, "Hola nutrióloga");
		when(patientMessageRepository.findThreadForPatient(eq(5L), isNull(), org.mockito.ArgumentMatchers.any()))
			.thenReturn(List.of(message));

		final CursorPagedResponse<PatientMessageSummaryDto> page = service.listMessages(5L, null, 20);

		assertThat(page.content()).hasSize(1);
		assertThat(page.content().get(0).id()).isEqualTo(42L);
		assertThat(page.content().get(0).body()).isEqualTo("Hola nutrióloga");
		assertThat(page.content().get(0).senderRole()).isEqualTo(MessageSenderRole.NUTRITIONIST);
		assertThat(page.hasMore()).isFalse();
		assertThat(page.nextCursor()).isNull();
	}

	@Test
	void listMessages_returnsNextCursorWhenMoreResultsExist() {
		final PatientMessage first = sampleMessage(100L, "Mensaje 1");
		final PatientMessage second = sampleMessage(99L, "Mensaje 2");
		final PatientMessage extra = sampleMessage(98L, "Mensaje 3");
		when(patientMessageRepository.findThreadForPatient(eq(5L), isNull(), org.mockito.ArgumentMatchers.any()))
			.thenAnswer(invocation -> {
				final Pageable pageable = invocation.getArgument(2);
				assertThat(pageable.getPageSize()).isEqualTo(3);
				return List.of(first, second, extra);
			});

		final CursorPagedResponse<PatientMessageSummaryDto> page = service.listMessages(5L, null, 2);

		assertThat(page.content()).hasSize(2);
		assertThat(page.hasMore()).isTrue();
		assertThat(page.nextCursor()).isEqualTo("99");
	}

	private static PatientMessage sampleMessage(final Long id, final String body) {
		final Paciente paciente = new Paciente();
		paciente.setId(5L);
		final PatientMessage message = new PatientMessage();
		message.setId(id);
		message.setPaciente(paciente);
		message.setNutritionistUserId("auth0|nutritionist");
		message.setSenderRole(MessageSenderRole.NUTRITIONIST);
		message.setBody(body);
		message.setSentAt(Instant.parse("2026-06-01T12:00:00Z"));
		message.setReadByPatient(false);
		return message;
	}

}
