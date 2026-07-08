package com.nutriconsultas.auth.apple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppleSignInNotificationServiceTest {

	@InjectMocks
	private AppleSignInNotificationService service;

	@Mock
	private AppleSignInProperties properties;

	@Mock
	private AppleSignInNotificationVerifier notificationVerifier;

	@Mock
	private AppleSignInNotificationRepository notificationRepository;

	@Mock
	private AppleIdentityMappingService identityMappingService;

	@Mock
	private AppleSignInAccountLifecycleService accountLifecycleService;

	@Mock
	private AppleSignInRelayEmailService relayEmailService;

	@Mock
	private AppleSignInWebhookObservability webhookObservability;

	@Test
	void handleNotificationPersistsVerifiedEventInObserveOnlyMode() {
		final AppleSignInNotificationClaims claims = sampleClaims(AppleSignInEventType.CONSENT_REVOKED);
		when(notificationVerifier.verifyAndParse("signed-payload")).thenReturn(claims);
		when(notificationRepository.findByAppleEventId("evt-1")).thenReturn(Optional.empty());
		when(properties.isAutoProcessDestructiveEvents()).thenReturn(false);
		when(identityMappingService.mapNotification("001234.abc", "relay@privaterelay.appleid.com"))
			.thenReturn(AppleIdentityMappingResult.mapped("apple|001234.abc", 42L));
		when(notificationRepository.save(any(AppleSignInNotification.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		final AppleSignInWebhookOutcome outcome = service.handleNotification("signed-payload");

		assertThat(outcome).isEqualTo(AppleSignInWebhookOutcome.PROCESSED);
		final ArgumentCaptor<AppleSignInNotification> captor = ArgumentCaptor.forClass(AppleSignInNotification.class);
		verify(notificationRepository).save(captor.capture());
		assertThat(captor.getValue().getProcessingStatus())
			.isEqualTo(AppleSignInNotificationProcessingStatus.PROCESSED);
		assertThat(captor.getValue().getLifecycleAction()).isEqualTo(AppleSignInLifecycleAction.SKIPPED_OBSERVE_ONLY);
		verify(accountLifecycleService, never()).applyDestructiveEvent(any(), any());
	}

	@Test
	void handleNotificationAppliesLifecycleWhenAutoProcessEnabled() {
		final AppleSignInNotificationClaims claims = sampleClaims(AppleSignInEventType.ACCOUNT_DELETE);
		when(notificationVerifier.verifyAndParse("signed-payload")).thenReturn(claims);
		when(notificationRepository.findByAppleEventId("evt-1")).thenReturn(Optional.empty());
		when(properties.isAutoProcessDestructiveEvents()).thenReturn(true);
		when(identityMappingService.mapNotification("001234.abc", "relay@privaterelay.appleid.com"))
			.thenReturn(AppleIdentityMappingResult.mapped("apple|001234.abc", 42L));
		when(accountLifecycleService.applyDestructiveEvent(any(AppleSignInNotification.class),
				eq(AppleSignInEventType.ACCOUNT_DELETE)))
			.thenReturn(AppleSignInLifecycleAction.APPLIED_PENDING_DELETION_REVIEW);
		when(notificationRepository.save(any(AppleSignInNotification.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		service.handleNotification("signed-payload");

		final ArgumentCaptor<AppleSignInNotification> captor = ArgumentCaptor.forClass(AppleSignInNotification.class);
		verify(notificationRepository).save(captor.capture());
		assertThat(captor.getValue().getLifecycleAction())
			.isEqualTo(AppleSignInLifecycleAction.APPLIED_PENDING_DELETION_REVIEW);
	}

	@Test
	void handleNotificationAppliesRelayEmailEvent() {
		final AppleSignInNotificationClaims claims = sampleClaims(AppleSignInEventType.EMAIL_ENABLED);
		when(notificationVerifier.verifyAndParse("signed-payload")).thenReturn(claims);
		when(notificationRepository.findByAppleEventId("evt-1")).thenReturn(Optional.empty());
		when(identityMappingService.mapNotification("001234.abc", "relay@privaterelay.appleid.com"))
			.thenReturn(AppleIdentityMappingResult.mapped("apple|001234.abc", 42L));
		when(relayEmailService.applyRelayEmailEvent(any(AppleSignInNotification.class), eq(claims)))
			.thenReturn(AppleSignInLifecycleAction.APPLIED_RELAY_FORWARDING_ENABLED);
		when(notificationRepository.save(any(AppleSignInNotification.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		service.handleNotification("signed-payload");

		final ArgumentCaptor<AppleSignInNotification> captor = ArgumentCaptor.forClass(AppleSignInNotification.class);
		verify(notificationRepository).save(captor.capture());
		assertThat(captor.getValue().getAppleSubject()).isEqualTo("001234.abc");
		assertThat(captor.getValue().getLifecycleAction())
			.isEqualTo(AppleSignInLifecycleAction.APPLIED_RELAY_FORWARDING_ENABLED);
		verify(relayEmailService).applyRelayEmailEvent(any(AppleSignInNotification.class), eq(claims));
		verify(accountLifecycleService, never()).applyDestructiveEvent(any(), any());
	}

	@Test
	void handleNotificationReturnsDuplicateWithoutSavingAgain() {
		final AppleSignInNotificationClaims claims = sampleClaims(AppleSignInEventType.EMAIL_ENABLED);
		when(notificationVerifier.verifyAndParse("signed-payload")).thenReturn(claims);
		when(notificationRepository.findByAppleEventId("evt-1")).thenReturn(Optional.of(new AppleSignInNotification()));

		final AppleSignInWebhookOutcome outcome = service.handleNotification("signed-payload");

		assertThat(outcome).isEqualTo(AppleSignInWebhookOutcome.DUPLICATE);
		verify(notificationRepository, never()).save(any());
	}

	@Test
	void handleNotificationMarksUnknownEventsIgnored() {
		final AppleSignInNotificationClaims claims = sampleClaims(AppleSignInEventType.UNKNOWN);
		when(notificationVerifier.verifyAndParse("signed-payload")).thenReturn(claims);
		when(notificationRepository.findByAppleEventId("evt-1")).thenReturn(Optional.empty());
		when(identityMappingService.mapNotification("001234.abc", "relay@privaterelay.appleid.com"))
			.thenReturn(AppleIdentityMappingResult.noAuth0User());
		when(notificationRepository.save(any(AppleSignInNotification.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		service.handleNotification("signed-payload");

		final ArgumentCaptor<AppleSignInNotification> captor = ArgumentCaptor.forClass(AppleSignInNotification.class);
		verify(notificationRepository).save(captor.capture());
		assertThat(captor.getValue().getProcessingStatus()).isEqualTo(AppleSignInNotificationProcessingStatus.IGNORED);
	}

	private static AppleSignInNotificationClaims sampleClaims(final AppleSignInEventType eventType) {
		return new AppleSignInNotificationClaims("evt-1", "https://appleid.apple.com", "com.minutriporcion.app",
				"001234.abc", eventType, "relay@privaterelay.appleid.com", true, true, "{\"jti\":\"evt-1\"}");
	}

}
