package com.nutriconsultas.subscription.lifecycle;

import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.nutriconsultas.subscription.SubscriptionProperties;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SubscriptionLifecycleJob {

	private final SubscriptionLifecycleService lifecycleService;

	private final SubscriptionProperties subscriptionProperties;

	public SubscriptionLifecycleJob(final SubscriptionLifecycleService lifecycleService,
			final SubscriptionProperties subscriptionProperties) {
		this.lifecycleService = lifecycleService;
		this.subscriptionProperties = subscriptionProperties;
	}

	@Scheduled(cron = "${nutriconsultas.subscription.lifecycle-job-cron:0 0 6 * * *}", zone = "America/Mexico_City")
	public void runDailyLifecycle() {
		if (!subscriptionProperties.isLifecycleJobEnabled()) {
			return;
		}
		try {
			lifecycleService.runDailyLifecycle(Instant.now());
		}
		catch (RuntimeException ex) {
			log.error("Subscription lifecycle job failed", ex);
		}
	}

}
