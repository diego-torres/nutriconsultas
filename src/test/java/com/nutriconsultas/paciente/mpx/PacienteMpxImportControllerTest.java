package com.nutriconsultas.paciente.mpx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.nutriconsultas.paciente.PacienteController;
import com.nutriconsultas.subscription.SubscriptionErrorResponses;
import com.nutriconsultas.subscription.SubscriptionLimitExceededException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@ActiveProfiles("test")
@SuppressWarnings("null")
class PacienteMpxImportControllerTest {

	private static final String USER_ID = "nutritionist-owner";

	@InjectMocks
	private PacienteController controller;

	@Mock
	private PacienteMpxImportService pacienteMpxImportService;

	@Mock
	private SubscriptionErrorResponses subscriptionErrorResponses;

	@Mock
	private OidcUser principal;

	private RedirectAttributes redirectAttributes;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(controller, "pacienteMpxImportService", pacienteMpxImportService);
		ReflectionTestUtils.setField(controller, "subscriptionErrorResponses", subscriptionErrorResponses);
		when(principal.getSubject()).thenReturn(USER_ID);
		redirectAttributes = new RedirectAttributesModelMap();
	}

	@Test
	void importPacienteMpx_redirectsToProfileOnSuccess() {
		final MockMultipartFile file = new MockMultipartFile("mpxFile", "juan.mpx", "application/x-yaml",
				new byte[] { 1 });
		when(pacienteMpxImportService.importRegistration(eq(file), eq(USER_ID)))
			.thenReturn(new MpxImportResult(12L, false));

		final String view = controller.importPacienteMpx(file, principal, redirectAttributes);

		assertThat(view).isEqualTo("redirect:/admin/pacientes/12");
		assertThat(redirectAttributes.getFlashAttributes().get("importSuccess"))
			.isEqualTo("Paciente importado correctamente");
	}

	@Test
	void importPacienteMpx_addsDuplicateWarningFlash() {
		final MockMultipartFile file = new MockMultipartFile("mpxFile", "juan.mpx", "application/x-yaml",
				new byte[] { 1 });
		when(pacienteMpxImportService.importRegistration(eq(file), eq(USER_ID)))
			.thenReturn(new MpxImportResult(12L, true));

		controller.importPacienteMpx(file, principal, redirectAttributes);

		assertThat(redirectAttributes.getFlashAttributes()).containsKey("importDuplicateWarning");
	}

	@Test
	void importPacienteMpx_redirectsWithErrorOnInvalidFile() {
		final MockMultipartFile file = new MockMultipartFile("mpxFile", "bad.mpx", "application/x-yaml",
				new byte[] { 1 });
		when(pacienteMpxImportService.importRegistration(any(), eq(USER_ID)))
			.thenThrow(new MpxImportException("El archivo no es un MPX válido"));

		final String view = controller.importPacienteMpx(file, principal, redirectAttributes);

		assertThat(view).isEqualTo("redirect:/admin/pacientes");
		assertThat(redirectAttributes.getFlashAttributes().get("importError"))
			.isEqualTo("El archivo no es un MPX válido");
	}

	@Test
	void importPacienteMpx_redirectsWithSubscriptionErrorWhenCapExceeded() {
		final MockMultipartFile file = new MockMultipartFile("mpxFile", "juan.mpx", "application/x-yaml",
				new byte[] { 1 });
		when(pacienteMpxImportService.importRegistration(any(), eq(USER_ID)))
			.thenThrow(new SubscriptionLimitExceededException("error.subscription.patient_limit"));
		when(subscriptionErrorResponses.resolve(any(SubscriptionLimitExceededException.class)))
			.thenReturn("Ha alcanzado el límite de pacientes de su plan");

		final String view = controller.importPacienteMpx(file, principal, redirectAttributes);

		assertThat(view).isEqualTo("redirect:/admin/pacientes");
		assertThat(redirectAttributes.getFlashAttributes().get("importError"))
			.isEqualTo("Ha alcanzado el límite de pacientes de su plan");
	}

	@Test
	void importPacienteMpx_redirectsWhenPrincipalMissing() {
		final MockMultipartFile file = new MockMultipartFile("mpxFile", "juan.mpx", "application/x-yaml",
				new byte[] { 1 });

		final String view = controller.importPacienteMpx(file, null, redirectAttributes);

		assertThat(view).isEqualTo("redirect:/admin/pacientes");
		assertThat(redirectAttributes.getFlashAttributes().get("importError"))
			.isEqualTo("No se pudo identificar al usuario");
	}

}
