package com.nutriconsultas.paciente.invitation;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.nutriconsultas.util.InvitationTokenHasher;

import lombok.extern.slf4j.Slf4j;

/**
 * Patient invitation token service (#133). Reuses {@link InvitationTokenHasher} for URL
 * token hashing and constant-time verification shared with subscription invitations.
 */
@Service
@Slf4j
public class PatientInvitationTokenServiceImpl implements PatientInvitationTokenService {

	private static final int SECRET_BYTES = 32;

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final PatientInvitationProperties properties;

	public PatientInvitationTokenServiceImpl(final PatientInvitationProperties properties) {
		this.properties = properties;
	}

	@Override
	public PatientInvitationTokenBundle generate() {
		final byte[] secret = new byte[SECRET_BYTES];
		SECURE_RANDOM.nextBytes(secret);
		final String urlToken = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
		final String humanCode = PatientInvitationHumanCode.format(secret, properties.getHumanCodePrefix());
		final String tokenHash = InvitationTokenHasher.hashToken(urlToken);
		if (log.isDebugEnabled()) {
			log.debug("Generated patient invitation token hash (patient onboarding)");
		}
		return new PatientInvitationTokenBundle(urlToken, humanCode, tokenHash);
	}

	@Override
	public boolean verify(final String rawUrlToken, final String storedTokenHash) {
		if (!StringUtils.hasText(rawUrlToken) || !StringUtils.hasText(storedTokenHash)) {
			return false;
		}
		return InvitationTokenHasher.verifyToken(rawUrlToken, storedTokenHash);
	}

	@Override
	public Optional<String> createOfflineJws(final Long pacienteId, final String rawUrlToken, final Instant expiresAt) {
		if (!properties.isJwsEnabled()) {
			return Optional.empty();
		}
		if (pacienteId == null || pacienteId < 1L || !StringUtils.hasText(rawUrlToken) || expiresAt == null) {
			throw new IllegalArgumentException("pacienteId, rawUrlToken, and expiresAt are required");
		}
		final String tokenHash = InvitationTokenHasher.hashToken(rawUrlToken);
		return Optional.of(PatientInvitationJws.sign(properties.getJwsSecret(), pacienteId, tokenHash, expiresAt));
	}

	@Override
	public Optional<Long> verifyOfflineJws(final String compactJws) {
		if (!properties.isJwsEnabled()) {
			return Optional.empty();
		}
		return PatientInvitationJws.verify(properties.getJwsSecret(), compactJws);
	}

}
