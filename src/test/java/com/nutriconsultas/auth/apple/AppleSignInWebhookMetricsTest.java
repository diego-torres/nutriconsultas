package com.nutriconsultas.auth.apple;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class AppleSignInWebhookMetricsTest {

	private SimpleMeterRegistry registry;

	private AppleSignInWebhookMetrics metrics;

	@BeforeEach
	void setUp() {
		registry = new SimpleMeterRegistry();
		metrics = new AppleSignInWebhookMetrics(registry);
	}

	@Test
	void recordsWebhookLifecycleCountersWithLowCardinalityTags() {
		metrics.recordReceived();
		metrics.recordVerified(AppleSignInEventType.CONSENT_REVOKED);
		metrics.recordFailed("invalid_signature");
		metrics.recordDuplicate(AppleSignInEventType.EMAIL_ENABLED);
		metrics.recordUnmapped(AppleSignInEventType.ACCOUNT_DELETE, AppleIdentityMappingStatus.NO_LOCAL_USER);

		assertThat(registry.get(AppleSignInWebhookMetrics.WEBHOOK_RECEIVED).counter().count()).isEqualTo(1.0);
		assertThat(registry.get(AppleSignInWebhookMetrics.WEBHOOK_VERIFIED)
			.tag("event_type", "CONSENT_REVOKED")
			.counter()
			.count()).isEqualTo(1.0);
		assertThat(registry.get(AppleSignInWebhookMetrics.WEBHOOK_FAILED)
			.tag("reason", "invalid_signature")
			.counter()
			.count()).isEqualTo(1.0);
		assertThat(registry.get(AppleSignInWebhookMetrics.WEBHOOK_DUPLICATE)
			.tag("event_type", "EMAIL_ENABLED")
			.counter()
			.count()).isEqualTo(1.0);
		assertThat(registry.get(AppleSignInWebhookMetrics.WEBHOOK_UNMAPPED)
			.tag("event_type", "ACCOUNT_DELETE")
			.tag("mapping_status", "NO_LOCAL_USER")
			.counter()
			.count()).isEqualTo(1.0);
		assertThat(registry.getMeters()).allSatisfy(meter -> {
			assertThat(meter.getId().getTags()).noneMatch(tag -> tag.getValue().contains("@"));
			assertThat(meter.getId().getTags()).noneMatch(tag -> tag.getValue().contains("apple|"));
		});
	}

}
