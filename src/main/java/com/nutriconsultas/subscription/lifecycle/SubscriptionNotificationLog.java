package com.nutriconsultas.subscription.lifecycle;

import java.time.Instant;

import com.nutriconsultas.subscription.Subscription;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "subscription_notification_log",
		uniqueConstraints = { @UniqueConstraint(name = "uk_subscription_notification",
				columnNames = { "subscription_id", "notification_type", "period_end_snapshot" }) })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionNotificationLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "subscription_id", nullable = false)
	private Subscription subscription;

	@Enumerated(EnumType.STRING)
	@Column(name = "notification_type", nullable = false, length = 40)
	private SubscriptionNotificationType notificationType;

	@Column(name = "period_end_snapshot", nullable = false)
	private Instant periodEndSnapshot;

	@Column(name = "sent_at", nullable = false, updatable = false)
	private Instant sentAt;

	@PrePersist
	void onCreate() {
		if (sentAt == null) {
			sentAt = Instant.now();
		}
	}

}
