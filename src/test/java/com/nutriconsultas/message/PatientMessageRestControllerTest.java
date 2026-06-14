package com.nutriconsultas.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import com.nutriconsultas.message.dto.PatientMessageThreadItemDto;
import com.nutriconsultas.message.dto.PatientUnreadMessageDto;
import com.nutriconsultas.message.dto.SendPatientMessageRequest;

@ExtendWith(MockitoExtension.class)
class PatientMessageRestControllerTest {

	private static final String USER_ID = "auth0|nutritionist";

	@InjectMocks
	private PatientMessageRestController controller;

	@Mock
	private PatientMessageService patientMessageService;

	@Test
	void listUnread_returnsSummaries() {
		when(patientMessageService.listUnreadSummaries(USER_ID)).thenReturn(List
			.of(new PatientUnreadMessageDto(1L, "Ana López", "Hola doctor", Instant.parse("2026-06-01T10:00:00Z"), 1)));

		final List<PatientUnreadMessageDto> result = controller.listUnread(oidcUser());

		assertThat(result).hasSize(1);
		assertThat(result.get(0).pacienteName()).isEqualTo("Ana López");
	}

	@Test
	void listThread_returnsMessages() {
		when(patientMessageService.listThread(eq(1L), eq(USER_ID)))
			.thenReturn(List.of(new PatientMessageThreadItemDto(5L, Instant.parse("2026-06-01T10:00:00Z"),
					MessageSenderRole.PATIENT, "Hola")));

		final List<PatientMessageThreadItemDto> result = controller.listThread(oidcUser(), 1L);

		assertThat(result.get(0).body()).isEqualTo("Hola");
	}

	@Test
	void sendMessage_returnsCreatedMessage() {
		when(patientMessageService.sendAsNutritionist(eq(1L), eq(USER_ID), eq("Respuesta")))
			.thenReturn(new PatientMessageThreadItemDto(9L, Instant.parse("2026-06-01T11:00:00Z"),
					MessageSenderRole.NUTRITIONIST, "Respuesta"));

		final PatientMessageThreadItemDto result = controller.sendMessage(oidcUser(), 1L,
				new SendPatientMessageRequest("Respuesta"));

		assertThat(result.body()).isEqualTo("Respuesta");
	}

	@Test
	void markRead_returnsOk() {
		final Map<String, String> result = controller.markRead(oidcUser(), 1L);

		verify(patientMessageService).markThreadReadByNutritionist(1L, USER_ID);
		assertThat(result.get("status")).isEqualTo("ok");
	}

	@Test
	void countUnread_returnsCount() {
		when(patientMessageService.countUnread(USER_ID)).thenReturn(3L);

		final Map<String, Long> result = controller.countUnread(oidcUser());

		assertThat(result.get("count")).isEqualTo(3L);
	}

	private static DefaultOidcUser oidcUser() {
		final OidcIdToken token = OidcIdToken.withTokenValue("token")
			.claim("sub", USER_ID)
			.claim("name", "Nutritionist")
			.build();
		return new DefaultOidcUser(List.of(), token);
	}

}
