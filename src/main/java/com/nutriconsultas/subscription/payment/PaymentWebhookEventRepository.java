package com.nutriconsultas.subscription.payment;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, Long> {

	Optional<PaymentWebhookEvent> findByProviderAndEventId(String provider, String eventId);

}
