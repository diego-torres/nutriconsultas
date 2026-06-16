package com.nutriconsultas.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import com.nutriconsultas.profile.NutritionistProfile;
import com.nutriconsultas.profile.NutritionistProfileRepository;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class MobilePatientMessageServiceTest {

	@InjectMocks
	private MobilePatientMessageService service;

	@Mock
	private PatientMessageRepository patientMessageRepository;

	@Mock
	private NutritionistProfileRepository nutritionistProfileRepository;

	@Mock
	private PatientWriteRateLimiter patientWriteRateLimiter;

	@Test
	void listMessages_returnsCursorPageWithoutNextWhenNoMore() {
		final PatientMessage message = sampleMessage(42L, "Hola nutrióloga");
		when(patientMessageRepository.findThreadForPatient(eq(5L), isNull(), org.mockito.ArgumentMatchers.any()))
			.thenReturn(List.of(message));
		when(nutritionistProfileRepository.findByUserId("auth0|nutritionist")).thenReturn(Optional.empty());

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
		when(nutritionistProfileRepository.findByUserId("auth0|nutritionist")).thenReturn(Optional.empty());

		final CursorPagedResponse<PatientMessageSummaryDto> page = service.listMessages(5L, null, 2);

		assertThat(page.content()).hasSize(2);
		assertThat(page.hasMore()).isTrue();
		assertThat(page.nextCursor()).isEqualTo("99");
	}

	@Test
	void listMessages_includesSenderDisplayNameForNutritionistMessages() {
		final PatientMessage message = sampleMessage(42L, "Hola nutrióloga");
		final NutritionistProfile profile = new NutritionistProfile();
		profile.setUserId("auth0|nutritionist");
		profile.setDisplayName("Lic. Ana López");
		when(patientMessageRepository.findThreadForPatient(eq(5L), isNull(), org.mockito.ArgumentMatchers.any()))
			.thenReturn(List.of(message));
		when(nutritionistProfileRepository.findByUserId("auth0|nutritionist")).thenReturn(Optional.of(profile));

		final CursorPagedResponse<PatientMessageSummaryDto> page = service.listMessages(5L, null, 20);

		assertThat(page.content().get(0).senderDisplayName()).isEqualTo("Lic. Ana López");
	}

	@Test
	void listMessages_omitsSenderDisplayNameWhenNoNutritionistProfile() {
		final PatientMessage message = sampleMessage(42L, "Hola nutrióloga");
		when(patientMessageRepository.findThreadForPatient(eq(5L), isNull(), org.mockito.ArgumentMatchers.any()))
			.thenReturn(List.of(message));
		when(nutritionistProfileRepository.findByUserId("auth0|nutritionist")).thenReturn(Optional.empty());

		final CursorPagedResponse<PatientMessageSummaryDto> page = service.listMessages(5L, null, 20);

		assertThat(page.content().get(0).senderDisplayName()).isNull();
	}

	@Test
	void sendMessage_doesNotIncludeSenderDisplayNameForPatientMessages() throws Exception {
		final Paciente paciente = new Paciente();
		paciente.setId(5L);
		paciente.setUserId("auth0|nutritionist-owner");
		paciente.setPatientAuthSub("auth0|mobile-message-patient");
		when(patientWriteRateLimiter.execute(eq(PatientWriteRateLimiter.PATIENT_MESSAGES),
				eq("auth0|mobile-message-patient"), any(Callable.class)))
			.thenAnswer(invocation -> {
				final Callable<PatientMessageSummaryDto> callable = invocation.getArgument(2);
				return callable.call();
			});
		when(patientMessageRepository.save(any(PatientMessage.class))).thenAnswer(invocation -> {
			final PatientMessage saved = invocation.getArgument(0);
			saved.setId(77L);
			saved.setSentAt(Instant.parse("2026-06-01T13:00:00Z"));
			saved.setReadByPatient(true);
			saved.setReadByNutritionist(false);
			return saved;
		});

		final PatientMessageSummaryDto sent = service.sendMessage(paciente, "  Hola doctor  ");

		assertThat(sent.id()).isEqualTo(77L);
		assertThat(sent.senderRole()).isEqualTo(MessageSenderRole.PATIENT);
		assertThat(sent.body()).isEqualTo("Hola doctor");
		assertThat(sent.read()).isTrue();
		assertThat(sent.senderDisplayName()).isNull();
		final ArgumentCaptor<PatientMessage> captor = ArgumentCaptor.forClass(PatientMessage.class);
		verify(patientMessageRepository).save(captor.capture());
		assertThat(captor.getValue().getSenderRole()).isEqualTo(MessageSenderRole.PATIENT);
		assertThat(captor.getValue().getNutritionistUserId()).isEqualTo("auth0|nutritionist-owner");
		assertThat(captor.getValue().getPaciente()).isSameAs(paciente);
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
