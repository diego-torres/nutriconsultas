package com.nutriconsultas.auth.apple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.auth0.Auth0ManagementUserService;
import com.nutriconsultas.paciente.ApplePacienteLifecycleStatus;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.PacienteStatus;

@ExtendWith(MockitoExtension.class)
class AppleSignInAccountLifecycleServiceTest {

	@InjectMocks
	private AppleSignInAccountLifecycleServiceImpl service;

	@Mock
	private PacienteRepository pacienteRepository;

	@Mock
	private Auth0ManagementUserService auth0ManagementUserService;

	@Test
	void applyDestructiveEventMarksAccessRevokedWithoutHardDelete() {
		final Paciente paciente = paciente(42L, ApplePacienteLifecycleStatus.NONE, PacienteStatus.ACTIVE);
		final AppleSignInNotification notification = notification(42L, "apple|001234.abc");
		when(pacienteRepository.findById(42L)).thenReturn(Optional.of(paciente));
		when(pacienteRepository.save(paciente)).thenReturn(paciente);
		when(auth0ManagementUserService.isConfigured()).thenReturn(true);

		final AppleSignInLifecycleAction action = service.applyDestructiveEvent(notification,
				AppleSignInEventType.CONSENT_REVOKED);

		assertThat(action).isEqualTo(AppleSignInLifecycleAction.APPLIED_ACCESS_REVOKED);
		assertThat(paciente.getAppleLifecycleStatus()).isEqualTo(ApplePacienteLifecycleStatus.ACCESS_REVOKED);
		assertThat(paciente.getStatus()).isEqualTo(PacienteStatus.REVOKED);
		verify(auth0ManagementUserService).updateAppMetadata(eq("apple|001234.abc"), any(Map.class));
		verify(auth0ManagementUserService).blockUserInAppMetadata("apple|001234.abc");
		verify(auth0ManagementUserService, never()).deleteUser(any());
	}

	@Test
	void applyDestructiveEventMarksPendingDeletionReviewForAccountDelete() {
		final Paciente paciente = paciente(7L, ApplePacienteLifecycleStatus.NONE, PacienteStatus.ACTIVE);
		final AppleSignInNotification notification = notification(7L, "apple|001234.abc");
		when(pacienteRepository.findById(7L)).thenReturn(Optional.of(paciente));
		when(pacienteRepository.save(paciente)).thenReturn(paciente);
		when(auth0ManagementUserService.isConfigured()).thenReturn(true);

		final AppleSignInLifecycleAction action = service.applyDestructiveEvent(notification,
				AppleSignInEventType.ACCOUNT_DELETE);

		assertThat(action).isEqualTo(AppleSignInLifecycleAction.APPLIED_PENDING_DELETION_REVIEW);
		assertThat(paciente.getAppleLifecycleStatus()).isEqualTo(ApplePacienteLifecycleStatus.PENDING_DELETION_REVIEW);
		verify(auth0ManagementUserService).updateAppMetadata(eq("apple|001234.abc"), any(Map.class));
		verify(auth0ManagementUserService, never()).blockUserInAppMetadata(any());
		verify(auth0ManagementUserService, never()).deleteUser(any());
	}

	@Test
	void applyDestructiveEventIsIdempotentWhenStatusAlreadyApplied() {
		final Paciente paciente = paciente(3L, ApplePacienteLifecycleStatus.ACCESS_REVOKED, PacienteStatus.REVOKED);
		final AppleSignInNotification notification = notification(3L, "apple|001234.abc");
		when(pacienteRepository.findById(3L)).thenReturn(Optional.of(paciente));

		final AppleSignInLifecycleAction action = service.applyDestructiveEvent(notification,
				AppleSignInEventType.CONSENT_REVOKED);

		assertThat(action).isEqualTo(AppleSignInLifecycleAction.ALREADY_APPLIED);
		verify(pacienteRepository, never()).save(any());
		verify(auth0ManagementUserService, never()).updateAppMetadata(any(), any());
	}

	@Test
	void applyDestructiveEventSkipsWhenPacienteNotMapped() {
		final AppleSignInNotification notification = notification(null, null);

		final AppleSignInLifecycleAction action = service.applyDestructiveEvent(notification,
				AppleSignInEventType.ACCOUNT_DELETE);

		assertThat(action).isEqualTo(AppleSignInLifecycleAction.SKIPPED_NO_PACIENTE);
		verify(pacienteRepository, never()).save(any());
	}

	private static Paciente paciente(final Long id, final ApplePacienteLifecycleStatus lifecycleStatus,
			final PacienteStatus status) {
		final Paciente paciente = new Paciente();
		paciente.setId(id);
		paciente.setName("Paciente Test");
		paciente.setUserId("nutritionist-sub");
		paciente.setAppleLifecycleStatus(lifecycleStatus);
		paciente.setStatus(status);
		return paciente;
	}

	private static AppleSignInNotification notification(final Long pacienteId, final String auth0UserId) {
		final AppleSignInNotification notification = new AppleSignInNotification();
		notification.setPacienteId(pacienteId);
		notification.setAuth0UserId(auth0UserId);
		notification.setAppleEventId("evt-1");
		return notification;
	}

}
