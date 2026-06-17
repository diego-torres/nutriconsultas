package com.nutriconsultas.subscription.lifecycle;

import java.time.Instant;

import com.nutriconsultas.subscription.Subscription;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionNotificationLogRepository extends JpaRepository<SubscriptionNotificationLog, Long> {

	boolean existsBySubscriptionAndNotificationTypeAndPeriodEndSnapshot(Subscription subscription,
			SubscriptionNotificationType notificationType, Instant periodEndSnapshot);

}
