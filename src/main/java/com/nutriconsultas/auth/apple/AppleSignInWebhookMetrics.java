package com.nutriconsultas.auth.apple;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Micrometer counters for Apple Sign-In webhook operations (#508). Tags are
 * low-cardinality only — never emails, Apple subjects, Auth0 user IDs, or patient data.
 */
@Component
public final class AppleSignInWebhookMetrics {

	static final String WEBHOOK_RECEIVED = "apple.signin.webhook.received";

	static final String WEBHOOK_VERIFIED = "apple.signin.webhook.verified";

	static final String WEBHOOK_FAILED = "apple.signin.webhook.failed";

	static final String WEBHOOK_DUPLICATE = "apple.signin.webhook.duplicate";

	static final String WEBHOOK_UNMAPPED = "apple.signin.webhook.unmapped";

	private static final String TAG_EVENT_TYPE = "event_type";

	private static final String TAG_REASON = "reason";

	private static final String TAG_MAPPING_STATUS = "mapping_status";

	private final MeterRegistry meterRegistry;

	public AppleSignInWebhookMetrics(final MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	public void recordReceived() {
		counter(WEBHOOK_RECEIVED).increment();
	}

	public void recordVerified(@Nullable final AppleSignInEventType eventType) {
		counter(WEBHOOK_VERIFIED, TAG_EVENT_TYPE, safeEnum(eventType)).increment();
	}

	public void recordFailed(final String reason) {
		counter(WEBHOOK_FAILED, TAG_REASON, safeTagValue(reason)).increment();
	}

	public void recordDuplicate(@Nullable final AppleSignInEventType eventType) {
		counter(WEBHOOK_DUPLICATE, TAG_EVENT_TYPE, safeEnum(eventType)).increment();
	}

	public void recordUnmapped(@Nullable final AppleSignInEventType eventType,
			@Nullable final AppleIdentityMappingStatus mappingStatus) {
		counter(WEBHOOK_UNMAPPED, TAG_EVENT_TYPE, safeEnum(eventType), TAG_MAPPING_STATUS, safeEnum(mappingStatus))
			.increment();
	}

	private Counter counter(final String name, final String... tags) {
		return meterRegistry.counter(name, tags);
	}

	private static String safeEnum(@Nullable final Enum<?> value) {
		if (value == null) {
			return "unknown";
		}
		return value.name();
	}

	private static String safeTagValue(@Nullable final String value) {
		if (!StringUtils.hasText(value)) {
			return "unknown";
		}
		return value.trim();
	}

}
