package com.nutriconsultas.paciente.invitation;

import java.time.Instant;
import java.util.Optional;

/**
 * CSPRNG invitation token generation and verification for patient onboarding (#133).
 */
public interface PatientInvitationTokenService {

	/**
	 * Generates a URL-safe token, human-readable code, and SHA-256 hash for persistence.
	 */
	PatientInvitationTokenBundle generate();

	/**
	 * Constant-time verification against a stored hash.
	 */
	boolean verify(String rawUrlToken, String storedTokenHash);

	/**
	 * Optional compact JWS for Auth0 Post-Login offline checks (#140). Authoritative
	 * redemption remains the DB gate (#136).
	 */
	Optional<String> createOfflineJws(Long pacienteId, String rawUrlToken, Instant expiresAt);

	/**
	 * Verifies offline JWS signature and expiry; returns embedded {@code patientId}.
	 */
	Optional<Long> verifyOfflineJws(String compactJws);

}
