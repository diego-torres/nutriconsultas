package com.nutriconsultas.subscription.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.nutriconsultas.subscription.PlanTier;
import com.nutriconsultas.subscription.Subscription;
import com.nutriconsultas.subscription.SubscriptionRepository;
import com.nutriconsultas.subscription.SubscriptionStatus;

@DataJpaTest
@ActiveProfiles("test")
class PaymentWebhookEventPersistenceTest {

	@Autowired
	private PaymentWebhookEventRepository webhookEventRepository;

	@Autowired
	private SubscriptionRepository subscriptionRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void webhookEventPersistsWithUniqueProviderEventId() {
		final Subscription subscription = subscriptionRepository.saveAndFlush(activeSubscription());

		final PaymentWebhookEvent event = new PaymentWebhookEvent();
		event.setProvider(PaymentProperties.PROVIDER_MERCADOPAGO);
		event.setEventId("req-1:pre-1");
		event.setEventType("subscription_preapproval");
		event.setSubscription(subscription);
		event.setProcessedAt(Instant.now());
		webhookEventRepository.saveAndFlush(event);

		entityManager.clear();

		assertThat(
				webhookEventRepository.findByProviderAndEventId(PaymentProperties.PROVIDER_MERCADOPAGO, "req-1:pre-1"))
			.isPresent();
	}

	private static Subscription activeSubscription() {
		final Subscription subscription = new Subscription();
		subscription.setPlanTier(PlanTier.BASICO);
		subscription.setStatus(SubscriptionStatus.PENDING_PAYMENT);
		subscription.setPaymentExempt(false);
		subscription.setGracePeriodDays(7);
		subscription.setExternalSubscriptionId("mp-sub-test");
		return subscription;
	}

}
