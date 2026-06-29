package com.nutriconsultas.mobile.auth;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.nutriconsultas.auth0.Auth0PatientAuthenticationClient;
import com.nutriconsultas.auth0.Auth0PatientAuthenticationException;
import com.nutriconsultas.auth0.Auth0PatientTokenResponse;
import com.nutriconsultas.mobile.PatientAuthBrokerNotConfiguredException;
import com.nutriconsultas.mobile.PatientAuthEmailMismatchException;
import com.nutriconsultas.mobile.dto.PatientAuthTokensDto;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PatientInvitation;
import com.nutriconsultas.paciente.PatientInvitationStatus;
import com.nutriconsultas.paciente.invitation.PatientInvitationHumanCodes;
import com.nutriconsultas.paciente.invitation.PatientInvitationProperties;
import com.nutriconsultas.paciente.invitation.PatientInvitationUrlTokens;
import com.nutriconsultas.util.InvitationTokenHasher;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PatientMobileAuthService {

	private final Auth0PatientAuthenticationClient auth0PatientAuthenticationClient;

	private final PatientInvitationAuthRepository invitationAuthRepository;

	private final PatientInvitationProperties invitationProperties;

	public PatientMobileAuthService(final Auth0PatientAuthenticationClient auth0PatientAuthenticationClient,
			final PatientInvitationAuthRepository invitationAuthRepository,
			final PatientInvitationProperties invitationProperties) {
		this.auth0PatientAuthenticationClient = auth0PatientAuthenticationClient;
		this.invitationAuthRepository = invitationAuthRepository;
		this.invitationProperties = invitationProperties;
	}

	public PatientAuthTokensDto signUp(final String email, final String password, final String displayName,
			final String rawUrlToken, final String humanCode) {
		requireBrokerConfigured();
		final PatientInvitation invitation = resolvePendingInvitation(rawUrlToken, humanCode);
		assertEmailMatchesInvitation(email, invitation);
		final String invitationToken = resolveInvitationToken(rawUrlToken, humanCode);
		final Map<String, String> metadata = StringUtils.hasText(displayName) ? Map.of("name", displayName.trim())
				: Map.of();
		auth0PatientAuthenticationClient.signUpDatabaseUser(normalizeEmail(email), password, metadata);
		return toDto(
				auth0PatientAuthenticationClient.loginWithPassword(normalizeEmail(email), password, invitationToken));
	}

	public PatientAuthTokensDto login(final String email, final String password, final String rawUrlToken,
			final String humanCode) {
		requireBrokerConfigured();
		final String invitationToken = resolveOptionalInvitationToken(rawUrlToken, humanCode);
		return toDto(
				auth0PatientAuthenticationClient.loginWithPassword(normalizeEmail(email), password, invitationToken));
	}

	private void requireBrokerConfigured() {
		if (!auth0PatientAuthenticationClient.isConfigured()) {
			throw new PatientAuthBrokerNotConfiguredException();
		}
	}

	private PatientInvitation resolvePendingInvitation(final String rawUrlToken, final String humanCode) {
		if (StringUtils.hasText(rawUrlToken)) {
			if (!PatientInvitationUrlTokens.isWellFormed(rawUrlToken)) {
				throw new com.nutriconsultas.mobile.PatientInvitationInvalidTokenException();
			}
			final String tokenHash = InvitationTokenHasher.hashToken(rawUrlToken);
			return invitationAuthRepository.findPendingByTokenHash(tokenHash)
				.orElseThrow(com.nutriconsultas.mobile.PatientInvitationUnavailableException::new);
		}
		if (StringUtils.hasText(humanCode)
				&& PatientInvitationHumanCodes.isWellFormed(humanCode, invitationProperties.getHumanCodePrefix())) {
			final String normalizedCode = PatientInvitationHumanCodes.normalize(humanCode);
			return invitationAuthRepository.findPendingByHumanCode(normalizedCode)
				.orElseThrow(com.nutriconsultas.mobile.PatientInvitationUnavailableException::new);
		}
		throw new com.nutriconsultas.mobile.PatientInvitationInvalidTokenException();
	}

	private String resolveOptionalInvitationToken(final String rawUrlToken, final String humanCode) {
		if (StringUtils.hasText(rawUrlToken)) {
			return rawUrlToken.trim();
		}
		if (StringUtils.hasText(humanCode)) {
			return PatientInvitationHumanCodes.normalize(humanCode);
		}
		return null;
	}

	private String resolveInvitationToken(final String rawUrlToken, final String humanCode) {
		if (StringUtils.hasText(rawUrlToken)) {
			return rawUrlToken.trim();
		}
		if (StringUtils.hasText(humanCode)) {
			return PatientInvitationHumanCodes.normalize(humanCode);
		}
		throw new com.nutriconsultas.mobile.PatientInvitationInvalidTokenException();
	}

	private void assertEmailMatchesInvitation(final String email, final PatientInvitation invitation) {
		final Paciente paciente = invitation.getPaciente();
		final String invitedEmail = paciente.getEmail();
		if (!StringUtils.hasText(invitedEmail)) {
			return;
		}
		if (!Objects.equals(normalizeEmail(email), normalizeEmail(invitedEmail))) {
			throw new PatientAuthEmailMismatchException();
		}
	}

	private String normalizeEmail(final String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private PatientAuthTokensDto toDto(final Auth0PatientTokenResponse tokens) {
		if (log.isDebugEnabled()) {
			log.debug("Patient mobile auth broker issued tokens at {}", Instant.now());
		}
		return PatientAuthTokensDto.from(tokens);
	}

}
