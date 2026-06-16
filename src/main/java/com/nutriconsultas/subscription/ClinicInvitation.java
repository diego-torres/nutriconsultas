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
 * Director-issued clinic invitation (no separate payment). Stores SHA-256 token hash
 * only.
 */
@Entity
@Table(name = "clinic_invitation")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClinicInvitation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "clinic_id", nullable = false)
	private Clinic clinic;

	@Column(nullable = false, length = 255)
	private String email;

	@Column(name = "token_hash", nullable = false, length = 64, unique = true)
	private String tokenHash;

	@Column(name = "invited_by_user_id", nullable = false, length = 255)
	private String invitedByUserId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private InvitationStatus status = InvitationStatus.PENDING;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "redeemed_at")
	private Instant redeemedAt;

	@Column(name = "redeemed_by_user_id", length = 255)
	private String redeemedByUserId;

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

}
