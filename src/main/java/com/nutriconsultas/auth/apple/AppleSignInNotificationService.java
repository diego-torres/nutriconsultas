package com.nutriconsultas.auth.apple;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AppleSignInNotificationService {

	private final AppleSignInProperties properties;

	private final AppleSignInNotificationVerifier notificationVerifier;

	private final AppleSignInNotificationRepository notificationRepository;

	private final AppleIdentityMappingService identityMappingService;

	private final AppleSignInAccountLifecycleService accountLifecycleService;

	private final AppleSignInRelayEmailService relayEmailService;

	private final AppleSignInWebhookObservability webhookObservability;

	public AppleSignInNotificationService(final AppleSignInProperties properties,
			final AppleSignInNotificationVerifier notificationVerifier,
			final AppleSignInNotificationRepository notificationRepository,
			final AppleIdentityMappingService identityMappingService,
			final AppleSignInAccountLifecycleService accountLifecycleService,
			final AppleSignInRelayEmailService relayEmailService,
			final AppleSignInWebhookObservability webhookObservability) {
		this.properties = properties;
		this.notificationVerifier = notificationVerifier;
		this.notificationRepository = notificationRepository;
		this.identityMappingService = identityMappingService;
		this.accountLifecycleService = accountLifecycleService;
		this.relayEmailService = relayEmailService;
		this.webhookObservability = webhookObservability;
	}

	@Transactional
	public AppleSignInWebhookOutcome handleNotification(final String signedPayload) {
		final AppleSignInNotificationClaims claims = notificationVerifier.verifyAndParse(signedPayload);
		webhookObservability.recordVerificationSuccess(claims);
		if (notificationRepository.findByAppleEventId(claims.eventId()).isPresent()) {
			webhookObservability.recordDuplicate(claims);
			if (log.isDebugEnabled()) {
				log.debug("Duplicate Apple sign-in notification eventId={} type={}", claims.eventId(),
						claims.eventType());
			}
			return AppleSignInWebhookOutcome.DUPLICATE;
		}
		final AppleSignInNotification notification = new AppleSignInNotification();
		notification.setAppleEventId(claims.eventId());
		notification.setEventType(claims.eventType());
		notification.setAppleSubject(claims.appleSubject());
		notification.setEmail(claims.email());
		notification.setEmailVerified(claims.emailVerified());
		notification.setIsPrivateEmail(claims.isPrivateEmail());
		notification.setRawClaimsJson(claims.rawClaimsJson());
		notification.setProcessingStatus(AppleSignInNotificationProcessingStatus.VERIFIED);
		notification.setReceivedAt(Instant.now());
		applyIdentityMapping(notification, claims);
		processNotification(notification, claims);
		notificationRepository.save(notification);
		webhookObservability.recordProcessed(notification, claims);
		if (log.isInfoEnabled()) {
			log.info("Processed Apple sign-in notification eventId={} type={} status={}", claims.eventId(),
					claims.eventType(), notification.getProcessingStatus());
		}
		return AppleSignInWebhookOutcome.PROCESSED;
	}

	private void applyIdentityMapping(final AppleSignInNotification notification,
			final AppleSignInNotificationClaims claims) {
		final AppleIdentityMappingResult mappingResult = identityMappingService.mapNotification(claims.appleSubject(),
				claims.email());
		notification.setIdentityMappingStatus(mappingResult.status());
		notification.setAuth0UserId(mappingResult.auth0UserId());
		notification.setPacienteId(mappingResult.pacienteId());
		if (StringUtils.hasText(mappingResult.detail())) {
			notification.setProcessingError(truncateDetail(mappingResult.detail()));
		}
		if (log.isDebugEnabled()) {
			log.debug("Apple identity mapping status={} pacienteId={}", mappingResult.status(),
					mappingResult.pacienteId());
		}
	}

	private void processNotification(final AppleSignInNotification notification,
			final AppleSignInNotificationClaims claims) {
		if (claims.eventType() == AppleSignInEventType.UNKNOWN) {
			notification.setProcessingStatus(AppleSignInNotificationProcessingStatus.IGNORED);
			notification.setProcessedAt(Instant.now());
			return;
		}
		if (claims.eventType().isRelayEmailEvent()) {
			final AppleSignInLifecycleAction relayAction = relayEmailService.applyRelayEmailEvent(notification, claims);
			notification.setLifecycleAction(relayAction);
			notification.setProcessingStatus(AppleSignInNotificationProcessingStatus.PROCESSED);
			notification.setProcessedAt(Instant.now());
			return;
		}
		if (claims.eventType().isDestructive() && !properties.isAutoProcessDestructiveEvents()) {
			notification.setLifecycleAction(AppleSignInLifecycleAction.SKIPPED_OBSERVE_ONLY);
			notification.setProcessingStatus(AppleSignInNotificationProcessingStatus.PROCESSED);
			notification.setProcessedAt(Instant.now());
			if (log.isInfoEnabled()) {
				log.info("Recorded destructive Apple sign-in event without auto-processing eventId={} type={}",
						claims.eventId(), claims.eventType());
			}
			return;
		}
		if (claims.eventType().isDestructive()) {
			final AppleSignInLifecycleAction lifecycleAction = accountLifecycleService
				.applyDestructiveEvent(notification, claims.eventType());
			notification.setLifecycleAction(lifecycleAction);
		}
		else {
			notification.setLifecycleAction(AppleSignInLifecycleAction.NOT_APPLICABLE);
		}
		notification.setProcessingStatus(AppleSignInNotificationProcessingStatus.PROCESSED);
		notification.setProcessedAt(Instant.now());
		if (StringUtils.hasText(claims.appleSubject()) && log.isDebugEnabled()) {
			log.debug("Apple sign-in observe-only processing complete for subject hash={}",
					claims.appleSubject().hashCode());
		}
	}

	private static String truncateDetail(final String detail) {
		if (detail == null || detail.length() <= 500) {
			return detail;
		}
		return detail.substring(0, 497) + "...";
	}

}
