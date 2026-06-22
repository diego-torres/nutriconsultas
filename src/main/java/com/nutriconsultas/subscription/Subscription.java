package com.nutriconsultas.subscription;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "subscription")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private PlanTier planTier;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private SubscriptionStatus status = SubscriptionStatus.PENDING_PAYMENT;

	@Column(name = "period_start")
	private Instant periodStart;

	@Column(name = "period_end")
	private Instant periodEnd;

	@Column(name = "payment_exempt", nullable = false)
	private boolean paymentExempt;

	@Column(name = "grace_period_days", nullable = false)
	private int gracePeriodDays = 7;

	@Column(name = "external_subscription_id", length = 255)
	private String externalSubscriptionId;

	@Column(name = "external_customer_id", length = 255)
	private String externalCustomerId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "tenant_purged_at")
	private Instant tenantPurgedAt;

	@PrePersist
	void onCreate() {
		final Instant now = Instant.now();
		if (createdAt == null) {
			createdAt = now;
		}
		if (updatedAt == null) {
			updatedAt = now;
		}
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

}
