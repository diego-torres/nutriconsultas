package com.nutriconsultas.paciente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@ActiveProfiles("test")
@SuppressWarnings("null")
class PacienteDeletionRestControllerTest {

	private static final String USER_ID = "nutritionist-owner";

	@InjectMocks
	private PacienteRestController controller;

	@Mock
	private PacienteDeletionService pacienteDeletionService;

	@Mock
	private OidcUser principal;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(controller, "pacienteDeletionService", pacienteDeletionService);
		when(principal.getSubject()).thenReturn(USER_ID);
	}

	@Test
	void deletePaciente_returnsOkWhenOwned() {
		final ResponseEntity<java.util.Map<String, Object>> response = controller.deletePaciente(7L, principal);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).containsEntry("success", true);
		verify(pacienteDeletionService).deletePatientWithHistory(7L, USER_ID);
	}

	@Test
	void deletePaciente_returnsNotFoundWhenPatientMissing() {
		doThrow(new IllegalArgumentException("Paciente no encontrado")).when(pacienteDeletionService)
			.deletePatientWithHistory(7L, USER_ID);

		final ResponseEntity<java.util.Map<String, Object>> response = controller.deletePaciente(7L, principal);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).containsEntry("success", false);
	}

	@Test
	void deletePaciente_returnsUnauthorizedWhenPrincipalMissing() {
		final ResponseEntity<java.util.Map<String, Object>> response = controller.deletePaciente(7L, null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		verify(pacienteDeletionService, org.mockito.Mockito.never()).deletePatientWithHistory(7L, USER_ID);
	}

}
