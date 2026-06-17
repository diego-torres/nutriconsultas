package com.nutriconsultas.subscription.lifecycle;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nutriconsultas.subscription.NutritionistInvitation;
import com.nutriconsultas.subscription.NutritionistInvitationRepository;
import com.nutriconsultas.subscription.Subscription;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SubscriptionNotificationService {

	private final SubscriptionNotificationLogRepository notificationLogRepository;

	private final SubscriptionNotificationEmailSender emailSender;

	private final NutritionistInvitationRepository invitationRepository;

	public SubscriptionNotificationService(final SubscriptionNotificationLogRepository notificationLogRepository,
			final SubscriptionNotificationEmailSender emailSender,
			final NutritionistInvitationRepository invitationRepository) {
		this.notificationLogRepository = notificationLogRepository;
		this.emailSender = emailSender;
		this.invitationRepository = invitationRepository;
	}

	@Transactional
	public boolean sendIfNotAlreadySent(final Subscription subscription,
			final SubscriptionNotificationType notificationType, final Instant periodEndSnapshot) {
		if (periodEndSnapshot == null) {
			return false;
		}
		if (notificationLogRepository.existsBySubscriptionAndNotificationTypeAndPeriodEndSnapshot(subscription,
				notificationType, periodEndSnapshot)) {
			return false;
		}
		final List<String> recipients = resolveNotificationRecipientEmails(subscription);
		for (final String recipientEmail : recipients) {
			if (StringUtils.hasText(recipientEmail)) {
				emailSender.sendNotification(recipientEmail, subscription, notificationType);
			}
		}
		final SubscriptionNotificationLog logEntry = new SubscriptionNotificationLog();
		logEntry.setSubscription(subscription);
		logEntry.setNotificationType(notificationType);
		logEntry.setPeriodEndSnapshot(periodEndSnapshot);
		notificationLogRepository.save(logEntry);
		if (log.isDebugEnabled()) {
			log.debug("Subscription notification sent: subscriptionId={}, type={}", subscription.getId(),
					notificationType);
		}
		return true;
	}

	private List<String> resolveNotificationRecipientEmails(final Subscription subscription) {
		return invitationRepository.findBySubscriptionId(subscription.getId())
			.map(NutritionistInvitation::getEmail)
			.stream()
			.toList();
	}

}
