package com.nutriconsultas.auth.apple;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Structured logging, metrics, and operator alerts for Apple Sign-In webhooks (#508).
 * Never logs emails, Apple subjects, Auth0 user IDs, or signed payloads.
 */
@Component
@Slf4j
public final class AppleSignInWebhookObservability {

	private static final String ALERT_VERIFICATION_FAILURES = "APPLE_SIGNIN_ALERT repeated_verification_failures";

	private static final String ALERT_UNMAPPED_DESTRUCTIVE = "APPLE_SIGNIN_ALERT unmapped_destructive_event";

	private final AppleSignInWebhookMetrics metrics;

	private final AppleSignInProperties properties;

	private int consecutiveVerificationFailures;

	public AppleSignInWebhookObservability(final AppleSignInWebhookMetrics metrics,
			final AppleSignInProperties properties) {
		this.metrics = metrics;
		this.properties = properties;
	}

	public void recordWebhookReceived() {
		metrics.recordReceived();
		if (log.isDebugEnabled()) {
			log.debug("apple_signin_webhook stage=received");
		}
	}

	public void recordVerificationSuccess(final AppleSignInNotificationClaims claims) {
		consecutiveVerificationFailures = 0;
		metrics.recordVerified(claims.eventType());
		if (log.isInfoEnabled()) {
			log.info("apple_signin_webhook stage=verified eventId={} eventType={}", claims.eventId(),
					claims.eventType());
		}
	}

	public void recordVerificationFailure(@Nullable final String message) {
		metrics.recordFailed(categorizeFailure(message));
		consecutiveVerificationFailures++;
		if (log.isWarnEnabled()) {
			log.warn("apple_signin_webhook stage=verification_failed reason={} consecutiveFailures={}",
					categorizeFailure(message), consecutiveVerificationFailures);
		}
		if (consecutiveVerificationFailures >= properties.getVerificationFailureAlertThreshold()
				&& log.isErrorEnabled()) {
			log.error("{} threshold={} consecutiveFailures={}", ALERT_VERIFICATION_FAILURES,
					properties.getVerificationFailureAlertThreshold(), consecutiveVerificationFailures);
		}
	}

	public void recordDuplicate(final AppleSignInNotificationClaims claims) {
		metrics.recordDuplicate(claims.eventType());
		if (log.isDebugEnabled()) {
			log.debug("apple_signin_webhook stage=duplicate eventId={} eventType={}", claims.eventId(),
					claims.eventType());
		}
	}

	public void recordProcessed(final AppleSignInNotification notification,
			final AppleSignInNotificationClaims claims) {
		if (log.isInfoEnabled()) {
			log.info(
					"apple_signin_webhook stage=processed eventId={} eventType={} processingStatus={} mappingStatus={} lifecycleAction={} pacienteId={}",
					claims.eventId(), claims.eventType(), notification.getProcessingStatus(),
					notification.getIdentityMappingStatus(), notification.getLifecycleAction(),
					notification.getPacienteId());
		}
		recordUnmappedDestructiveIfNeeded(notification, claims);
		recordAuth0Outcome(notification);
	}

	private void recordUnmappedDestructiveIfNeeded(final AppleSignInNotification notification,
			final AppleSignInNotificationClaims claims) {
		if (!claims.eventType().isDestructive()) {
			return;
		}
		if (notification.getIdentityMappingStatus() == AppleIdentityMappingStatus.MAPPED) {
			return;
		}
		metrics.recordUnmapped(claims.eventType(), notification.getIdentityMappingStatus());
		if (log.isWarnEnabled()) {
			log.warn("{} eventId={} eventType={} mappingStatus={} processingStatus={}", ALERT_UNMAPPED_DESTRUCTIVE,
					claims.eventId(), claims.eventType(), notification.getIdentityMappingStatus(),
					notification.getProcessingStatus());
		}
	}

	private void recordAuth0Outcome(final AppleSignInNotification notification) {
		if (notification.getLifecycleAction() != AppleSignInLifecycleAction.AUTH0_UPDATE_FAILED) {
			return;
		}
		if (log.isWarnEnabled()) {
			log.warn("apple_signin_webhook stage=auth0_update_failed eventId={} eventType={} pacienteId={}",
					notification.getAppleEventId(), notification.getEventType(), notification.getPacienteId());
		}
	}

	static String categorizeFailure(@Nullable final String message) {
		if (!StringUtils.hasText(message)) {
			return "verification";
		}
		final String normalized = message.trim().toLowerCase();
		if (normalized.contains("payload is required")) {
			return "missing_payload";
		}
		if (normalized.contains("signature")) {
			return "invalid_signature";
		}
		if (normalized.contains("unable to parse")) {
			return "parse_error";
		}
		if (normalized.contains("issuer") || normalized.contains("audience") || normalized.contains("jti")
				|| normalized.contains("events claim")) {
			return "invalid_claims";
		}
		return "verification";
	}

}
