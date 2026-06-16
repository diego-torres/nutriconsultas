package com.nutriconsultas.subscription;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Audit trail for subscription admin actions and payment webhooks. Never store PHI.
 */
@Entity
@Table(name = "subscription_audit_event")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionAuditEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "subscription_id")
	private Subscription subscription;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, length = 40)
	private SubscriptionAuditEventType eventType;

	@Column(name = "actor_user_id", length = 255)
	private String actorUserId;

	@Enumerated(EnumType.STRING)
	@Column(name = "previous_status", length = 30)
	private SubscriptionStatus previousStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "new_status", length = 30)
	private SubscriptionStatus newStatus;

	@Column(name = "reason_code", length = 50)
	private String reasonCode;

	@Column(length = 500)
	private String details;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

}
