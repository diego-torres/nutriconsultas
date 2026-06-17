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
 * Platform-admin paid onboarding invitation. Stores SHA-256 token hash only.
 */
@Entity
@Table(name = "nutritionist_invitation")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NutritionistInvitation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 255)
	private String email;

	@Column(name = "token_hash", nullable = false, length = 64, unique = true)
	private String tokenHash;

	@Enumerated(EnumType.STRING)
	@Column(name = "plan_tier", nullable = false, length = 30)
	private PlanTier planTier;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private InvitationStatus status = InvitationStatus.PENDING;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "created_by_user_id", nullable = false, length = 255)
	private String createdByUserId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "redeemed_at")
	private Instant redeemedAt;

	@Column(name = "redeemed_by_user_id", length = 255)
	private String redeemedByUserId;

	@ManyToOne
	@JoinColumn(name = "subscription_id")
	private Subscription subscription;

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

}
