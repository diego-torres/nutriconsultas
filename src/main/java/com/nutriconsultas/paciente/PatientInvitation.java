package com.nutriconsultas.paciente;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Nutritionist-issued patient onboarding invitation (#132). Stores SHA-256 token hash
 * only — never the raw token.
 */
@Entity
@Table(name = "patient_invitation")
@Getter
@Setter
@NoArgsConstructor
public class PatientInvitation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "token_hash", nullable = false, length = 64, unique = true)
	private String tokenHash;

	@Column(name = "human_code", length = 20, unique = true)
	private String humanCode;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "paciente_id", nullable = false)
	private Paciente paciente;

	/**
	 * Issuing nutritionist Auth0 {@code sub} — same value as
	 * {@link Paciente#getUserId()}.
	 */
	@Column(name = "nutritionist_user_id", nullable = false, length = 255)
	private String nutritionistUserId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PatientInvitationStatus status = PatientInvitationStatus.PENDING;

	@Column(name = "redeemed_by_sub", length = 255)
	private String redeemedBySub;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "max_uses", nullable = false)
	private int maxUses = 1;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "redeemed_at")
	private Instant redeemedAt;

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

}
