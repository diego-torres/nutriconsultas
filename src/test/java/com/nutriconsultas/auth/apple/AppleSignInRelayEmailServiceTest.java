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
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;

@ExtendWith(MockitoExtension.class)
class AppleSignInRelayEmailServiceTest {

	@InjectMocks
	private AppleSignInRelayEmailServiceImpl service;

	@Mock
	private PacienteRepository pacienteRepository;

	@Mock
	private Auth0ManagementUserService auth0ManagementUserService;

	@Test
	void applyRelayEmailEventDisablesForwardingAndUpdatesRelayMetadata() {
		final Paciente paciente = paciente(42L, "relay@privaterelay.appleid.com", null, true);
		final AppleSignInNotification notification = notification(42L, "apple|001234.abc");
		final AppleSignInNotificationClaims claims = claims(AppleSignInEventType.EMAIL_DISABLED,
				"relay@privaterelay.appleid.com", true);
		when(pacienteRepository.findById(42L)).thenReturn(Optional.of(paciente));
		when(pacienteRepository.save(paciente)).thenReturn(paciente);
		when(auth0ManagementUserService.isConfigured()).thenReturn(true);

		final AppleSignInLifecycleAction action = service.applyRelayEmailEvent(notification, claims);

		assertThat(action).isEqualTo(AppleSignInLifecycleAction.APPLIED_RELAY_FORWARDING_DISABLED);
		assertThat(paciente.getAppleRelayEmail()).isEqualTo("relay@privaterelay.appleid.com");
		assertThat(paciente.getAppleRelayPrivateEmail()).isTrue();
		assertThat(paciente.getAppleRelayForwardingEnabled()).isFalse();
		assertThat(paciente.getAppleRelayUpdatedAt()).isNotNull();
		assertThat(paciente.getEmail()).isEqualTo("relay@privaterelay.appleid.com");
		verify(auth0ManagementUserService).updateAppMetadata(eq("apple|001234.abc"), any(Map.class));
	}

	@Test
	void applyRelayEmailEventPreservesVerifiedNonRelayContactEmail() {
		final Paciente paciente = paciente(7L, "relay@privaterelay.appleid.com", "paciente@example.com", true);
		final AppleSignInNotification notification = notification(7L, "apple|001234.abc");
		final AppleSignInNotificationClaims claims = claims(AppleSignInEventType.EMAIL_DISABLED,
				"new-relay@privaterelay.appleid.com", true);
		when(pacienteRepository.findById(7L)).thenReturn(Optional.of(paciente));
		when(pacienteRepository.save(paciente)).thenReturn(paciente);
		when(auth0ManagementUserService.isConfigured()).thenReturn(false);

		final AppleSignInLifecycleAction action = service.applyRelayEmailEvent(notification, claims);

		assertThat(action).isEqualTo(AppleSignInLifecycleAction.APPLIED_RELAY_FORWARDING_DISABLED_CONTACT_PROTECTED);
		assertThat(paciente.getAppleRelayEmail()).isEqualTo("new-relay@privaterelay.appleid.com");
		assertThat(paciente.getEmail()).isEqualTo("paciente@example.com");
	}

	@Test
	void applyRelayEmailEventEnablesForwarding() {
		final Paciente paciente = paciente(3L, "relay@privaterelay.appleid.com", null, false);
		final AppleSignInNotification notification = notification(3L, "apple|001234.abc");
		final AppleSignInNotificationClaims claims = claims(AppleSignInEventType.EMAIL_ENABLED,
				"relay@privaterelay.appleid.com", true);
		when(pacienteRepository.findById(3L)).thenReturn(Optional.of(paciente));
		when(pacienteRepository.save(paciente)).thenReturn(paciente);
		when(auth0ManagementUserService.isConfigured()).thenReturn(false);

		final AppleSignInLifecycleAction action = service.applyRelayEmailEvent(notification, claims);

		assertThat(action).isEqualTo(AppleSignInLifecycleAction.APPLIED_RELAY_FORWARDING_ENABLED);
		assertThat(paciente.getAppleRelayForwardingEnabled()).isTrue();
	}

	@Test
	void applyRelayEmailEventIsIdempotentWhenStateAlreadyApplied() {
		final Paciente paciente = paciente(5L, "relay@privaterelay.appleid.com", "relay@privaterelay.appleid.com",
				false);
		final AppleSignInNotification notification = notification(5L, "apple|001234.abc");
		final AppleSignInNotificationClaims claims = claims(AppleSignInEventType.EMAIL_DISABLED,
				"relay@privaterelay.appleid.com", true);
		when(pacienteRepository.findById(5L)).thenReturn(Optional.of(paciente));

		final AppleSignInLifecycleAction action = service.applyRelayEmailEvent(notification, claims);

		assertThat(action).isEqualTo(AppleSignInLifecycleAction.ALREADY_APPLIED);
		verify(pacienteRepository, never()).save(any());
	}

	@Test
	void applyRelayEmailEventSkipsWhenPacienteNotMapped() {
		final AppleSignInNotification notification = notification(null, "apple|001234.abc");
		final AppleSignInNotificationClaims claims = claims(AppleSignInEventType.EMAIL_DISABLED,
				"relay@privaterelay.appleid.com", true);

		final AppleSignInLifecycleAction action = service.applyRelayEmailEvent(notification, claims);

		assertThat(action).isEqualTo(AppleSignInLifecycleAction.SKIPPED_NO_PACIENTE);
		verify(pacienteRepository, never()).save(any());
	}

	private static Paciente paciente(final Long id, final String relayEmail, final String contactEmail,
			final Boolean forwardingEnabled) {
		final Paciente paciente = new Paciente();
		paciente.setId(id);
		paciente.setAppleRelayEmail(relayEmail);
		paciente.setAppleRelayForwardingEnabled(forwardingEnabled);
		paciente.setEmail(contactEmail != null ? contactEmail : relayEmail);
		return paciente;
	}

	private static AppleSignInNotification notification(final Long pacienteId, final String auth0UserId) {
		final AppleSignInNotification notification = new AppleSignInNotification();
		notification.setPacienteId(pacienteId);
		notification.setAuth0UserId(auth0UserId);
		notification.setAppleEventId("evt-relay-1");
		return notification;
	}

	private static AppleSignInNotificationClaims claims(final AppleSignInEventType eventType, final String email,
			final boolean isPrivateEmail) {
		return new AppleSignInNotificationClaims("evt-relay-1", "https://appleid.apple.com", "com.minutriporcion.app",
				"001234.abc", eventType, email, true, isPrivateEmail, "{\"jti\":\"evt-relay-1\"}");
	}

}
