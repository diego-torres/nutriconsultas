package com.nutriconsultas.subscription.lifecycle;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.subscription.Subscription;
import com.nutriconsultas.subscription.SubscriptionAuditEvent;
import com.nutriconsultas.subscription.SubscriptionAuditEventRepository;
import com.nutriconsultas.subscription.SubscriptionAuditEventType;
import com.nutriconsultas.subscription.SubscriptionProperties;
import com.nutriconsultas.subscription.SubscriptionRepository;
import com.nutriconsultas.subscription.SubscriptionStatus;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SubscriptionLifecycleService {

	private final SubscriptionRepository subscriptionRepository;

	private final SubscriptionAuditEventRepository auditEventRepository;

	private final SubscriptionNotificationService notificationService;

	private final SubscriptionProperties subscriptionProperties;

	public SubscriptionLifecycleService(final SubscriptionRepository subscriptionRepository,
			final SubscriptionAuditEventRepository auditEventRepository,
			final SubscriptionNotificationService notificationService,
			final SubscriptionProperties subscriptionProperties) {
		this.subscriptionRepository = subscriptionRepository;
		this.auditEventRepository = auditEventRepository;
		this.notificationService = notificationService;
		this.subscriptionProperties = subscriptionProperties;
	}

	@Transactional
	public LifecycleRunResult runDailyLifecycle(final Instant now) {
		int graceTransitions = 0;
		int suspendedTransitions = 0;
		int remindersSent = 0;

		for (final Subscription subscription : subscriptionRepository
			.findByStatusAndPeriodEndBefore(SubscriptionStatus.ACTIVE, now)) {
			transitionToGrace(subscription, "SCHEDULED_JOB");
			graceTransitions++;
		}

		for (final Subscription subscription : subscriptionRepository.findByStatus(SubscriptionStatus.GRACE)) {
			if (isGraceExpired(subscription, now)) {
				transitionToSuspended(subscription, "SCHEDULED_JOB");
				suspendedTransitions++;
			}
		}

		for (final int daysBefore : subscriptionProperties.getExpiryReminderDays()) {
			remindersSent += sendExpiryRemindersForDay(now, daysBefore);
		}

		if (log.isInfoEnabled()) {
			log.info("Subscription lifecycle job completed: grace={}, suspended={}, reminders={}", graceTransitions,
					suspendedTransitions, remindersSent);
		}
		return new LifecycleRunResult(graceTransitions, suspendedTransitions, remindersSent);
	}

	@Transactional
	public Subscription applyAdminOverride(final String actorUserId, final Long subscriptionId,
			final AdminSubscriptionOverride override) {
		final Subscription subscription = subscriptionRepository.findById(subscriptionId)
			.orElseThrow(() -> new IllegalArgumentException("Subscription not found"));
		final SubscriptionStatus previousStatus = subscription.getStatus();
		boolean changed = false;

		if (override.paymentExempt() != null && subscription.isPaymentExempt() != override.paymentExempt()) {
			subscription.setPaymentExempt(override.paymentExempt());
			changed = true;
		}
		if (override.periodEnd() != null && !override.periodEnd().equals(subscription.getPeriodEnd())) {
			subscription.setPeriodEnd(override.periodEnd());
			changed = true;
		}
		if (override.gracePeriodDays() != null && subscription.getGracePeriodDays() != override.gracePeriodDays()) {
			subscription.setGracePeriodDays(override.gracePeriodDays());
			changed = true;
		}
		if (override.status() != null && subscription.getStatus() != override.status()) {
			subscription.setStatus(override.status());
			changed = true;
		}

		if (!changed) {
			return subscription;
		}

		final Subscription saved = subscriptionRepository.save(subscription);
		recordAudit(saved, actorUserId, previousStatus, SubscriptionAuditEventType.ADMIN_PAYMENT_OVERRIDE,
				override.reasonCode(), override.details());
		return saved;
	}

	private int sendExpiryRemindersForDay(final Instant now, final int daysBefore) {
		final LocalDate targetDay = LocalDate.ofInstant(now, ZoneOffset.UTC).plusDays(daysBefore);
		final Instant windowStart = targetDay.atStartOfDay().toInstant(ZoneOffset.UTC);
		final Instant windowEnd = targetDay.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
		int sent = 0;
		for (final Subscription subscription : subscriptionRepository
			.findActiveExpiringBetween(SubscriptionStatus.ACTIVE, windowStart, windowEnd)) {
			final SubscriptionNotificationType type = notificationTypeForDays(daysBefore);
			if (notificationService.sendIfNotAlreadySent(subscription, type, subscription.getPeriodEnd())) {
				sent++;
			}
		}
		return sent;
	}

	private void transitionToGrace(final Subscription subscription, final String reasonCode) {
		final SubscriptionStatus previousStatus = subscription.getStatus();
		subscription.setStatus(SubscriptionStatus.GRACE);
		subscriptionRepository.save(subscription);
		recordAudit(subscription, null, previousStatus, SubscriptionAuditEventType.STATE_TRANSITION, reasonCode,
				"Subscription entered grace period");
		notificationService.sendIfNotAlreadySent(subscription, SubscriptionNotificationType.GRACE_PERIOD_ENTERED,
				subscription.getPeriodEnd());
	}

	private void transitionToSuspended(final Subscription subscription, final String reasonCode) {
		final SubscriptionStatus previousStatus = subscription.getStatus();
		subscription.setStatus(SubscriptionStatus.SUSPENDED);
		subscriptionRepository.save(subscription);
		recordAudit(subscription, null, previousStatus, SubscriptionAuditEventType.STATE_TRANSITION, reasonCode,
				"Subscription suspended after grace period");
	}

	private static boolean isGraceExpired(final Subscription subscription, final Instant now) {
		if (subscription.getPeriodEnd() == null) {
			return false;
		}
		final Instant graceEndsAt = subscription.getPeriodEnd()
			.plus(subscription.getGracePeriodDays(), ChronoUnit.DAYS);
		return !graceEndsAt.isAfter(now);
	}

	private void recordAudit(final Subscription subscription, final String actorUserId,
			final SubscriptionStatus previousStatus, final SubscriptionAuditEventType eventType,
			final String reasonCode, final String details) {
		final SubscriptionAuditEvent auditEvent = new SubscriptionAuditEvent();
		auditEvent.setSubscription(subscription);
		auditEvent.setActorUserId(actorUserId);
		auditEvent.setEventType(eventType);
		auditEvent.setPreviousStatus(previousStatus);
		auditEvent.setNewStatus(subscription.getStatus());
		auditEvent.setReasonCode(reasonCode);
		auditEvent.setDetails(details);
		auditEventRepository.save(auditEvent);
	}

	private static SubscriptionNotificationType notificationTypeForDays(final int daysBefore) {
		return switch (daysBefore) {
			case 7 -> SubscriptionNotificationType.EXPIRY_REMINDER_7_DAYS;
			case 3 -> SubscriptionNotificationType.EXPIRY_REMINDER_3_DAYS;
			case 1 -> SubscriptionNotificationType.EXPIRY_REMINDER_1_DAY;
			default -> throw new IllegalArgumentException("Unsupported reminder day offset: " + daysBefore);
		};
	}

}
