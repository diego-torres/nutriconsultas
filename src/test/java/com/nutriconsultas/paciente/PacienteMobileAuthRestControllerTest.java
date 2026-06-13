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

import com.nutriconsultas.mobile.PatientAuthLinkageService;
import com.nutriconsultas.mobile.PatientMobileAuthStatus;

@ExtendWith(MockitoExtension.class)
class PacienteMobileAuthRestControllerTest {

	private static final String NUTRITIONIST_SUB = "auth0|nutritionist-rest";

	@InjectMocks
	private PacienteMobileAuthRestController controller;

	@Mock
	private PatientAuthLinkageService patientAuthLinkageService;

	@Test
	void getStatus_returnsLinkageInfo() {
		when(patientAuthLinkageService.getStatus(eq(1L), eq(NUTRITIONIST_SUB)))
			.thenReturn(PatientMobileAuthStatus.of("auth0|linked-patient", true));

		final ResponseEntity<Map<String, Object>> response = controller.getStatus(1L, principal());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).containsEntry("success", true).containsEntry("linked", true);
	}

	@Test
	void linkBySub_delegatesToService() {
		when(patientAuthLinkageService.getStatus(eq(2L), eq(NUTRITIONIST_SUB)))
			.thenReturn(PatientMobileAuthStatus.of("auth0|new-patient", false));

		final ResponseEntity<Map<String, Object>> response = controller.link(2L,
				Map.of("patientAuthSub", "auth0|new-patient"), principal());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		verify(patientAuthLinkageService).linkBySub(2L, NUTRITIONIST_SUB, "auth0|new-patient");
	}

	@Test
	void unlink_delegatesToService() {
		when(patientAuthLinkageService.getStatus(eq(3L), eq(NUTRITIONIST_SUB)))
			.thenReturn(PatientMobileAuthStatus.of(null, false));

		final ResponseEntity<Map<String, Object>> response = controller.unlink(3L, principal());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).containsEntry("linked", false);
		verify(patientAuthLinkageService).unlink(3L, NUTRITIONIST_SUB);
	}

	private static OidcUser principal() {
		final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(NUTRITIONIST_SUB).build();
		final OidcIdToken idToken = new OidcIdToken("token", jwt.getIssuedAt(), jwt.getExpiresAt(),
				Map.of("sub", NUTRITIONIST_SUB));
		return new DefaultOidcUser(null, idToken);
	}

}
