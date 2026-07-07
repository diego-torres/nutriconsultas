package com.nutriconsultas.auth.apple;

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

/**
 * Audit and idempotency store for Apple Sign-In server notifications (#502). Never store
 * signed tokens or refresh tokens.
 */
@Entity
@Table(name = "apple_signin_notification")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppleSignInNotification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "apple_event_id", nullable = false, length = 255, unique = true)
	private String appleEventId;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, length = 40)
	private AppleSignInEventType eventType;

	@Column(name = "apple_subject", length = 255)
	private String appleSubject;

	@Column(name = "auth0_user_id", length = 255)
	private String auth0UserId;

	@Column(name = "paciente_id")
	private Long pacienteId;

	@Enumerated(EnumType.STRING)
	@Column(name = "identity_mapping_status", length = 30)
	private AppleIdentityMappingStatus identityMappingStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "lifecycle_action", length = 40)
	private AppleSignInLifecycleAction lifecycleAction;

	@Column(length = 320)
	private String email;

	@Column(name = "email_verified")
	private Boolean emailVerified;

	@Column(name = "is_private_email")
	private Boolean isPrivateEmail;

	@Column(name = "raw_claims_json", columnDefinition = "TEXT")
	private String rawClaimsJson;

	@Enumerated(EnumType.STRING)
	@Column(name = "processing_status", nullable = false, length = 20)
	private AppleSignInNotificationProcessingStatus processingStatus;

	@Column(name = "processing_error", length = 500)
	private String processingError;

	@Column(name = "received_at", nullable = false)
	private Instant receivedAt;

	@Column(name = "processed_at")
	private Instant processedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@PrePersist
	void onCreate() {
		final Instant now = Instant.now();
		if (createdAt == null) {
			createdAt = now;
		}
		if (updatedAt == null) {
			updatedAt = now;
		}
		if (receivedAt == null) {
			receivedAt = now;
		}
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

}
