package com.nutriconsultas.subscription.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.subscription.PlanTier;
import com.nutriconsultas.subscription.Subscription;
import com.nutriconsultas.subscription.SubscriptionAuditEvent;
import com.nutriconsultas.subscription.SubscriptionAuditEventRepository;
import com.nutriconsultas.subscription.SubscriptionAuditEventType;
import com.nutriconsultas.subscription.SubscriptionProperties;
import com.nutriconsultas.subscription.SubscriptionRepository;
import com.nutriconsultas.subscription.SubscriptionStatus;

@ExtendWith(MockitoExtension.class)
class SubscriptionLifecycleServiceTest {

	@Mock
	private SubscriptionRepository subscriptionRepository;

	@Mock
	private SubscriptionAuditEventRepository auditEventRepository;

	@Mock
	private SubscriptionNotificationService notificationService;

	private SubscriptionProperties subscriptionProperties;

	private SubscriptionLifecycleService service;

	@BeforeEach
	void setUp() {
		subscriptionProperties = new SubscriptionProperties();
		service = new SubscriptionLifecycleService(subscriptionRepository, auditEventRepository, notificationService,
				subscriptionProperties);
	}

	@Test
	void runDailyLifecycleTransitionsActiveToGrace() {
		final Instant now = Instant.parse("2026-06-17T12:00:00Z");
		final Subscription subscription = activeSubscription(now.minus(1, ChronoUnit.DAYS));
		when(subscriptionRepository.findByStatusAndPeriodEndBefore(SubscriptionStatus.ACTIVE, now))
			.thenReturn(List.of(subscription));
		when(subscriptionRepository.findByStatus(SubscriptionStatus.GRACE)).thenReturn(List.of());
		when(subscriptionRepository.save(subscription)).thenReturn(subscription);

		final LifecycleRunResult result = service.runDailyLifecycle(now);

		assertThat(result.graceTransitions()).isEqualTo(1);
		assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.GRACE);
		verify(notificationService).sendIfNotAlreadySent(subscription,
				SubscriptionNotificationType.GRACE_PERIOD_ENTERED, subscription.getPeriodEnd());
	}

	@Test
	void runDailyLifecycleTransitionsGraceToSuspended() {
		final Instant now = Instant.parse("2026-06-17T12:00:00Z");
		final Subscription subscription = graceSubscription(now.minus(10, ChronoUnit.DAYS), 7);
		when(subscriptionRepository.findByStatusAndPeriodEndBefore(SubscriptionStatus.ACTIVE, now))
			.thenReturn(List.of());
		when(subscriptionRepository.findByStatus(SubscriptionStatus.GRACE)).thenReturn(List.of(subscription));
		when(subscriptionRepository.save(subscription)).thenReturn(subscription);

		final LifecycleRunResult result = service.runDailyLifecycle(now);

		assertThat(result.suspendedTransitions()).isEqualTo(1);
		assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.SUSPENDED);
	}

	@Test
	void applyAdminOverrideRecordsAudit() {
		final Subscription subscription = activeSubscription(Instant.now().plus(5, ChronoUnit.DAYS));
		when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));
		when(subscriptionRepository.save(subscription)).thenReturn(subscription);

		final Instant newEnd = Instant.now().plus(60, ChronoUnit.DAYS);
		final AdminSubscriptionOverride override = new AdminSubscriptionOverride(true, newEnd, 14,
				SubscriptionStatus.TRIAL, "TRIAL_EXTENSION", "Prórroga comercial");

		service.applyAdminOverride("auth0|admin", 1L, override);

		assertThat(subscription.isPaymentExempt()).isTrue();
		assertThat(subscription.getPeriodEnd()).isEqualTo(newEnd);
		assertThat(subscription.getGracePeriodDays()).isEqualTo(14);
		assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.TRIAL);

		final ArgumentCaptor<SubscriptionAuditEvent> captor = ArgumentCaptor.forClass(SubscriptionAuditEvent.class);
		verify(auditEventRepository).save(captor.capture());
		assertThat(captor.getValue().getEventType()).isEqualTo(SubscriptionAuditEventType.ADMIN_PAYMENT_OVERRIDE);
		assertThat(captor.getValue().getActorUserId()).isEqualTo("auth0|admin");
	}

	@Test
	void applyAdminOverrideNoOpWhenUnchanged() {
		final Subscription subscription = activeSubscription(Instant.now().plus(5, ChronoUnit.DAYS));
		when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));

		final AdminSubscriptionOverride override = new AdminSubscriptionOverride(subscription.isPaymentExempt(),
				subscription.getPeriodEnd(), subscription.getGracePeriodDays(), subscription.getStatus(), "NO_CHANGE",
				null);

		service.applyAdminOverride("auth0|admin", 1L, override);

		verify(subscriptionRepository, never()).save(any());
		verify(auditEventRepository, never()).save(any());
	}

	private static Subscription activeSubscription(final Instant periodEnd) {
		final Subscription subscription = new Subscription();
		subscription.setId(1L);
		subscription.setPlanTier(PlanTier.BASICO);
		subscription.setStatus(SubscriptionStatus.ACTIVE);
		subscription.setPeriodEnd(periodEnd);
		subscription.setGracePeriodDays(7);
		return subscription;
	}

	private static Subscription graceSubscription(final Instant periodEnd, final int graceDays) {
		final Subscription subscription = activeSubscription(periodEnd);
		subscription.setStatus(SubscriptionStatus.GRACE);
		subscription.setGracePeriodDays(graceDays);
		return subscription;
	}

}
