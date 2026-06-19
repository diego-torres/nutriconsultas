package com.nutriconsultas.paciente.mpx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

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

import com.nutriconsultas.paciente.PacienteController;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@ActiveProfiles("test")
@SuppressWarnings("null")
class PacienteMpxExportControllerTest {

	private static final String USER_ID = "nutritionist-owner";

	@InjectMocks
	private PacienteController controller;

	@Mock
	private PacienteMpxExportService pacienteMpxExportService;

	@Mock
	private OidcUser principal;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(controller, "pacienteMpxExportService", pacienteMpxExportService);
		when(principal.getSubject()).thenReturn(USER_ID);
	}

	@Test
	void exportPacienteMpx_returnsAttachmentWhenOwned() {
		final byte[] yaml = "mpxVersion: 1\n".getBytes(StandardCharsets.UTF_8);
		when(pacienteMpxExportService.exportRegistration(7L, USER_ID))
			.thenReturn(new MpxExportResult(yaml, "juan-20260618-120000.mpx"));

		final ResponseEntity<byte[]> response = controller.exportPacienteMpx(7L, principal);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getHeaders().getFirst("Content-Disposition"))
			.isEqualTo("attachment; filename=\"juan-20260618-120000.mpx\"");
		assertThat(response.getBody()).isEqualTo(yaml);
	}

	@Test
	void exportPacienteMpx_returnsNotFoundWhenPatientMissing() {
		when(pacienteMpxExportService.exportRegistration(7L, USER_ID))
			.thenThrow(new IllegalArgumentException("Paciente no encontrado"));

		final ResponseEntity<byte[]> response = controller.exportPacienteMpx(7L, principal);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void exportPacienteMpx_returnsUnauthorizedWhenPrincipalMissing() {
		final ResponseEntity<byte[]> response = controller.exportPacienteMpx(7L, null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

}
