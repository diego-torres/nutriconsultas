package com.nutriconsultas.auth.apple;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class AppleSignInWebhookObservabilityTest {

	private AppleSignInProperties properties;

	private AppleSignInWebhookObservability observability;

	private SimpleMeterRegistry registry;

	@BeforeEach
	void setUp() {
		properties = new AppleSignInProperties();
		properties.setVerificationFailureAlertThreshold(3);
		registry = new SimpleMeterRegistry();
		observability = new AppleSignInWebhookObservability(new AppleSignInWebhookMetrics(registry), properties);
	}

	@Test
	void categorizesVerificationFailureReasonsWithoutLoggingSecrets() {
		assertThat(AppleSignInWebhookObservability.categorizeFailure("Invalid Apple notification signature"))
			.isEqualTo("invalid_signature");
		assertThat(AppleSignInWebhookObservability.categorizeFailure("Invalid Apple notification audience"))
			.isEqualTo("invalid_claims");
		assertThat(AppleSignInWebhookObservability.categorizeFailure(null)).isEqualTo("verification");
	}

	@Test
	void recordsUnmappedDestructiveEvents() {
		final AppleSignInNotification notification = new AppleSignInNotification();
		notification.setIdentityMappingStatus(AppleIdentityMappingStatus.NO_LOCAL_USER);
		notification.setProcessingStatus(AppleSignInNotificationProcessingStatus.PROCESSED);
		notification.setLifecycleAction(AppleSignInLifecycleAction.SKIPPED_OBSERVE_ONLY);
		final AppleSignInNotificationClaims claims = new AppleSignInNotificationClaims("evt-1",
				"https://appleid.apple.com", "com.minutriporcion.app", "001234.abc",
				AppleSignInEventType.ACCOUNT_DELETE, null, null, null, "{}");

		observability.recordProcessed(notification, claims);

		assertThat(registry.get(AppleSignInWebhookMetrics.WEBHOOK_UNMAPPED)
			.tag("event_type", "ACCOUNT_DELETE")
			.tag("mapping_status", "NO_LOCAL_USER")
			.counter()
			.count()).isEqualTo(1.0);
	}

	@Test
	void skipsUnmappedMetricWhenDestructiveEventIsMapped() {
		final AppleSignInNotification notification = new AppleSignInNotification();
		notification.setIdentityMappingStatus(AppleIdentityMappingStatus.MAPPED);
		notification.setPacienteId(42L);
		notification.setProcessingStatus(AppleSignInNotificationProcessingStatus.PROCESSED);
		final AppleSignInNotificationClaims claims = new AppleSignInNotificationClaims("evt-2",
				"https://appleid.apple.com", "com.minutriporcion.app", "001234.abc",
				AppleSignInEventType.CONSENT_REVOKED, null, null, null, "{}");

		observability.recordProcessed(notification, claims);

		assertThat(registry.find(AppleSignInWebhookMetrics.WEBHOOK_UNMAPPED).counters()).isEmpty();
	}

	@Test
	void incrementsVerificationFailureCounter() {
		observability.recordVerificationFailure("Invalid Apple notification signature");
		observability.recordVerificationFailure("Invalid Apple notification audience");

		assertThat(registry.get(AppleSignInWebhookMetrics.WEBHOOK_FAILED)
			.tag("reason", "invalid_signature")
			.counter()
			.count()).isEqualTo(1.0);
		assertThat(registry.get(AppleSignInWebhookMetrics.WEBHOOK_FAILED)
			.tag("reason", "invalid_claims")
			.counter()
			.count()).isEqualTo(1.0);
	}

	@Test
	void resetsVerificationFailureStreakAfterSuccess() {
		observability.recordVerificationFailure("Invalid Apple notification signature");
		observability.recordVerificationFailure("Invalid Apple notification signature");
		observability.recordVerificationSuccess(sampleClaims());

		observability.recordVerificationFailure("Invalid Apple notification signature");

		assertThat(registry.get(AppleSignInWebhookMetrics.WEBHOOK_FAILED)
			.tag("reason", "invalid_signature")
			.counter()
			.count()).isEqualTo(3.0);
	}

	private static AppleSignInNotificationClaims sampleClaims() {
		return new AppleSignInNotificationClaims("evt-ok", "https://appleid.apple.com", "com.minutriporcion.app",
				"001234.abc", AppleSignInEventType.EMAIL_ENABLED, null, null, null, "{}");
	}

}
