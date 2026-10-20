package com.nutriconsultas.mobile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import com.nutriconsultas.auth0.Auth0ManagementApiException;
import com.nutriconsultas.auth0.Auth0ManagementUserService;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteDeletionService;
import com.nutriconsultas.paciente.PacienteRepository;

@ExtendWith(MockitoExtension.class)
class MobileAccountDeletionServiceTest {

	private static final String SUBJECT = "auth0|patient";

	@InjectMocks
	private MobileAccountDeletionService service;

	@Mock
	private PacienteRepository pacienteRepository;

	@Mock
	private PacienteDeletionService pacienteDeletionService;

	@Mock
	private Auth0ManagementUserService auth0ManagementUserService;

	@Test
	void deleteAuthenticatedAccount_removesLinkedPatientThenAuth0User() {
		final Paciente paciente = new Paciente();
		paciente.setId(35L);
		paciente.setUserId("nutritionist-sub");
		paciente.setPatientAuthSub(SUBJECT);
		when(pacienteRepository.findByPatientAuthSub(SUBJECT)).thenReturn(Optional.of(paciente));

		service.deleteAuthenticatedAccount(jwt(SUBJECT));

		final InOrder order = inOrder(pacienteDeletionService, auth0ManagementUserService);
		order.verify(pacienteDeletionService).deletePatientWithHistory(35L, "nutritionist-sub");
		order.verify(auth0ManagementUserService).deleteUserForAccountClosure(SUBJECT);
	}

	@Test
	void deleteAuthenticatedAccount_deletesAuth0UserWhenUnlinked() {
		when(pacienteRepository.findByPatientAuthSub(SUBJECT)).thenReturn(Optional.empty());

		service.deleteAuthenticatedAccount(jwt(SUBJECT));

		verify(pacienteDeletionService, never()).deletePatientWithHistory(anyLong(), anyString());
		verify(auth0ManagementUserService).deleteUserForAccountClosure(SUBJECT);
	}

	@Test
	void deleteAuthenticatedAccount_whenAuth0Fails_propagatesAndKeepsCallerAbleToRollBack() {
		when(pacienteRepository.findByPatientAuthSub(SUBJECT)).thenReturn(Optional.empty());
		doThrow(new Auth0ManagementApiException("denied")).when(auth0ManagementUserService)
			.deleteUserForAccountClosure(SUBJECT);

		assertThatThrownBy(() -> service.deleteAuthenticatedAccount(jwt(SUBJECT)))
			.isInstanceOf(AccountDeletionUnavailableException.class);
	}

	@Test
	void deleteAuthenticatedAccount_whenJwtMissing_isUnavailable() {
		assertThatThrownBy(() -> service.deleteAuthenticatedAccount(null))
			.isInstanceOf(AccountDeletionUnavailableException.class);

		verify(auth0ManagementUserService, never()).deleteUserForAccountClosure(any());
	}

	private static Jwt jwt(final String subject) {
		return Jwt.withTokenValue("token").header("alg", "none").subject(subject).build();
	}

}
