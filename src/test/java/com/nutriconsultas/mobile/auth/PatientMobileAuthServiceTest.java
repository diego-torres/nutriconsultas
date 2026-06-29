package com.nutriconsultas.mobile.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.auth0.Auth0PatientAuthenticationClient;
import com.nutriconsultas.auth0.Auth0PatientTokenResponse;
import com.nutriconsultas.mobile.PatientAuthEmailMismatchException;
import com.nutriconsultas.mobile.dto.PatientAuthTokensDto;
import com.nutriconsultas.mobile.dto.PatientSignupRequest;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PatientInvitation;
import com.nutriconsultas.paciente.PatientInvitationStatus;
import com.nutriconsultas.paciente.invitation.PatientInvitationProperties;

@ExtendWith(MockitoExtension.class)
class PatientMobileAuthServiceTest {

	private static final String HUMAN_CODE = "NUTRI-Z7V0-0P2S";

	@Mock
	private Auth0PatientAuthenticationClient auth0PatientAuthenticationClient;

	@Mock
	private PatientInvitationAuthRepository invitationAuthRepository;

	private PatientMobileAuthService service;

	@BeforeEach
	void setUp() {
		final PatientInvitationProperties properties = new PatientInvitationProperties();
		service = new PatientMobileAuthService(auth0PatientAuthenticationClient, invitationAuthRepository, properties);
		when(auth0PatientAuthenticationClient.isConfigured()).thenReturn(true);
	}

	@Test
	void signUpValidatesEmailAgainstInvitation() {
		final PatientInvitation invitation = invitation(HUMAN_CODE, "patient@example.com");
		when(invitationAuthRepository.findPendingByHumanCode(HUMAN_CODE)).thenReturn(java.util.Optional.of(invitation));

		assertThatThrownBy(() -> service
			.signUp(new PatientSignupRequest("other@example.com", "Test@1234", "Patient", null, HUMAN_CODE)))
			.isInstanceOf(PatientAuthEmailMismatchException.class);
	}

	@Test
	void signUpCreatesAuth0UserAndReturnsTokens() {
		final PatientInvitation invitation = invitation(HUMAN_CODE, "patient@example.com");
		when(invitationAuthRepository.findPendingByHumanCode(HUMAN_CODE)).thenReturn(java.util.Optional.of(invitation));
		when(auth0PatientAuthenticationClient.loginWithPassword(eq("patient@example.com"), eq("Test@1234"),
				eq(HUMAN_CODE)))
			.thenReturn(new Auth0PatientTokenResponse("access", "id", "refresh", 3600L, "Bearer"));

		final PatientAuthTokensDto tokens = service
			.signUp(new PatientSignupRequest("patient@example.com", "Test@1234", "Patient Name", null, HUMAN_CODE));

		verify(auth0PatientAuthenticationClient).signUpDatabaseUser(eq("patient@example.com"), eq("Test@1234"),
				eq(Map.of("name", "Patient Name")));
		assertThat(tokens.accessToken()).isEqualTo("access");
		assertThat(tokens.idToken()).isEqualTo("id");
	}

	private PatientInvitation invitation(final String humanCode, final String email) {
		final Paciente paciente = new Paciente();
		paciente.setEmail(email);
		final PatientInvitation invitation = new PatientInvitation();
		invitation.setHumanCode(humanCode);
		invitation.setStatus(PatientInvitationStatus.PENDING);
		invitation.setPaciente(paciente);
		invitation.setExpiresAt(java.time.Instant.now().plusSeconds(3600));
		return invitation;
	}

}
