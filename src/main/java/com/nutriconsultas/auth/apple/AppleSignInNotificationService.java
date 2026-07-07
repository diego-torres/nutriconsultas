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

	public AppleSignInNotificationService(final AppleSignInProperties properties,
			final AppleSignInNotificationVerifier notificationVerifier,
			final AppleSignInNotificationRepository notificationRepository) {
		this.properties = properties;
		this.notificationVerifier = notificationVerifier;
		this.notificationRepository = notificationRepository;
	}

	@Transactional
	public AppleSignInWebhookOutcome handleNotification(final String signedPayload) {
		final AppleSignInNotificationClaims claims = notificationVerifier.verifyAndParse(signedPayload);
		if (notificationRepository.findByAppleEventId(claims.eventId()).isPresent()) {
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
		processNotification(notification, claims);
		notificationRepository.save(notification);
		if (log.isInfoEnabled()) {
			log.info("Processed Apple sign-in notification eventId={} type={} status={}", claims.eventId(),
					claims.eventType(), notification.getProcessingStatus());
		}
		return AppleSignInWebhookOutcome.PROCESSED;
	}

	private void processNotification(final AppleSignInNotification notification,
			final AppleSignInNotificationClaims claims) {
		if (claims.eventType() == AppleSignInEventType.UNKNOWN) {
			notification.setProcessingStatus(AppleSignInNotificationProcessingStatus.IGNORED);
			notification.setProcessedAt(Instant.now());
			return;
		}
		if (claims.eventType().isDestructive() && !properties.isAutoProcessDestructiveEvents()) {
			notification.setProcessingStatus(AppleSignInNotificationProcessingStatus.PROCESSED);
			notification.setProcessedAt(Instant.now());
			if (log.isInfoEnabled()) {
				log.info("Recorded destructive Apple sign-in event without auto-processing eventId={} type={}",
						claims.eventId(), claims.eventType());
			}
			return;
		}
		notification.setProcessingStatus(AppleSignInNotificationProcessingStatus.PROCESSED);
		notification.setProcessedAt(Instant.now());
		if (StringUtils.hasText(claims.appleSubject()) && log.isDebugEnabled()) {
			log.debug("Apple sign-in observe-only processing complete for subject hash={}",
					claims.appleSubject().hashCode());
		}
	}

}
