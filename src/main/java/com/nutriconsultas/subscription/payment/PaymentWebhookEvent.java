package com.nutriconsultas.subscription.payment;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import com.nutriconsultas.subscription.Subscription;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Idempotency store for payment provider webhooks. Never store card data or PHI.
 */
@Entity
@Table(name = "payment_webhook_event")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentWebhookEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 30)
	private String provider;

	@Column(name = "event_id", nullable = false, length = 255)
	private String eventId;

	@Column(name = "event_type", length = 80)
	private String eventType;

	@ManyToOne
	@JoinColumn(name = "subscription_id")
	private Subscription subscription;

	@Column(name = "processed_at", nullable = false)
	private Instant processedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	void onCreate() {
		final Instant now = Instant.now();
		if (createdAt == null) {
			createdAt = now;
		}
		if (processedAt == null) {
			processedAt = now;
		}
	}

}
