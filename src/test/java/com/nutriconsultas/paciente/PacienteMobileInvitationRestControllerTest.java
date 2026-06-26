package com.nutriconsultas.paciente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
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

import com.nutriconsultas.paciente.invitation.IssuedPatientMobileInvitationResult;
import com.nutriconsultas.paciente.invitation.PatientInvitationRevokeResult;
import com.nutriconsultas.paciente.invitation.PatientMobileInvitationNotAllowedException;
import com.nutriconsultas.paciente.invitation.PatientMobileInvitationService;
import com.nutriconsultas.paciente.invitation.PatientMobileInvitationStatus;

@ExtendWith(MockitoExtension.class)
class PacienteMobileInvitationRestControllerTest {

	private static final String NUTRITIONIST_SUB = "auth0|nutritionist-web-invite";

	@InjectMocks
	private PacienteMobileInvitationRestController controller;

	@Mock
	private PatientMobileInvitationService patientMobileInvitationService;

	@Test
	void getStatus_returnsInvitationState() {
		final PatientMobileInvitationStatus status = new PatientMobileInvitationStatus("NONE", "Sin app", true, false,
				false, null, null, null, "j***@example.com");
		when(patientMobileInvitationService.getStatus(eq(1L), eq(NUTRITIONIST_SUB))).thenReturn(status);

		final ResponseEntity<Map<String, Object>> response = controller.getStatus(1L, principal());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).containsEntry("success", true)
			.containsEntry("stateCode", "NONE")
			.containsEntry("canSend", true);
	}

	@Test
	void sendInvitation_returnsCreated() {
		final Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
		when(patientMobileInvitationService.sendInvitation(eq(2L), eq(NUTRITIONIST_SUB))).thenReturn(
				new IssuedPatientMobileInvitationResult(10L, 2L, "NUTRI-ABCD-EFGH", expiresAt, "j***@example.com"));

		final ResponseEntity<Map<String, Object>> response = controller.sendInvitation(2L, principal());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).containsEntry("success", true).containsEntry("humanCode", "NUTRI-ABCD-EFGH");
	}

	@Test
	void sendInvitation_returnsBadRequestWhenNotAllowed() {
		when(patientMobileInvitationService.sendInvitation(eq(3L), eq(NUTRITIONIST_SUB)))
			.thenThrow(new PatientMobileInvitationNotAllowedException("NO_EMAIL"));

		final ResponseEntity<Map<String, Object>> response = controller.sendInvitation(3L, principal());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).containsEntry("error", "NO_EMAIL");
	}

	@Test
	void revokeInvitation_delegatesToService() {
		when(patientMobileInvitationService.revokePendingInvitation(eq(4L), eq(NUTRITIONIST_SUB)))
			.thenReturn(new PatientInvitationRevokeResult(99L, 4L, PatientInvitationStatus.REVOKED));

		final ResponseEntity<Map<String, Object>> response = controller.revokeInvitation(4L, principal());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).containsEntry("success", true).containsEntry("invitationId", 99L);
		verify(patientMobileInvitationService).revokePendingInvitation(4L, NUTRITIONIST_SUB);
	}

	private static OidcUser principal() {
		final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(NUTRITIONIST_SUB).build();
		final OidcIdToken idToken = new OidcIdToken("token", jwt.getIssuedAt(), jwt.getExpiresAt(),
				Map.of("sub", NUTRITIONIST_SUB));
		return new DefaultOidcUser(null, idToken);
	}

}
