package com.nutriconsultas.paciente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import com.nutriconsultas.paciente.preview.PacienteMobilePreviewService;
import com.nutriconsultas.paciente.preview.PatientMobilePreviewDto;

@ExtendWith(MockitoExtension.class)
class PacienteMobilePreviewRestControllerTest {

	private static final String NUTRITIONIST_SUB = "auth0|nutritionist-preview";

	@InjectMocks
	private PacienteMobilePreviewRestController controller;

	@Mock
	private PacienteMobilePreviewService pacienteMobilePreviewService;

	@Test
	void getPreview_returnsSuccessPayload() {
		final PatientMobilePreviewDto preview = new PatientMobilePreviewDto("María", "Lic. Ana", null, null, null, null,
				null);
		when(pacienteMobilePreviewService.buildPreview(eq(1L), eq(NUTRITIONIST_SUB))).thenReturn(preview);

		final ResponseEntity<Map<String, Object>> response = controller.getPreview(1L, principal());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).containsEntry("success", true)
			.containsEntry("firstName", "María")
			.containsEntry("nutritionistDisplayName", "Lic. Ana");
		verify(pacienteMobilePreviewService).buildPreview(1L, NUTRITIONIST_SUB);
	}

	@Test
	void getPreview_returnsNotFoundWhenPatientMissing() {
		when(pacienteMobilePreviewService.buildPreview(eq(2L), eq(NUTRITIONIST_SUB)))
			.thenThrow(new IllegalArgumentException("Paciente no encontrado"));

		final ResponseEntity<Map<String, Object>> response = controller.getPreview(2L, principal());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).containsEntry("success", false).containsEntry("error", "Paciente no encontrado");
	}

	private static OidcUser principal() {
		final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(NUTRITIONIST_SUB).build();
		final OidcIdToken idToken = new OidcIdToken("token", jwt.getIssuedAt(), jwt.getExpiresAt(),
				Map.of("sub", NUTRITIONIST_SUB));
		return new DefaultOidcUser(null, idToken);
	}

}
