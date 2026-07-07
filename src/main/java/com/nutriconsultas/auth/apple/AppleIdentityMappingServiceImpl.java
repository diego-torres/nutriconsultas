package com.nutriconsultas.auth.apple;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nutriconsultas.auth0.Auth0AppleIdentitySupport;
import com.nutriconsultas.auth0.Auth0ManagementApiException;
import com.nutriconsultas.auth0.Auth0ManagementUser;
import com.nutriconsultas.auth0.Auth0ManagementUserService;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AppleIdentityMappingServiceImpl implements AppleIdentityMappingService {

	private final Auth0ManagementUserService auth0ManagementUserService;

	private final PacienteRepository pacienteRepository;

	public AppleIdentityMappingServiceImpl(final Auth0ManagementUserService auth0ManagementUserService,
			final PacienteRepository pacienteRepository) {
		this.auth0ManagementUserService = auth0ManagementUserService;
		this.pacienteRepository = pacienteRepository;
	}

	@Override
	@Transactional
	public AppleIdentityMappingResult mapNotification(final String appleSubject, final String email) {
		if (!StringUtils.hasText(appleSubject)) {
			return AppleIdentityMappingResult.noAppleSubject();
		}
		final String normalizedSubject = appleSubject.trim();
		final Optional<Paciente> pacienteByAppleSubject = pacienteRepository.findByAppleSubject(normalizedSubject);
		if (pacienteByAppleSubject.isPresent()) {
			final Paciente paciente = pacienteByAppleSubject.get();
			return AppleIdentityMappingResult.mapped(paciente.getPatientAuthSub(), paciente.getId());
		}
		try {
			final Auth0Resolution auth0Resolution = resolveAuth0UserId(normalizedSubject, email);
			if (auth0Resolution.failureStatus() != null) {
				return new AppleIdentityMappingResult(auth0Resolution.failureStatus(), null, null,
						auth0Resolution.detail());
			}
			if (auth0Resolution.userId().isEmpty()) {
				return AppleIdentityMappingResult.noAuth0User();
			}
			final String resolvedAuth0UserId = auth0Resolution.userId().get();
			final Optional<Paciente> pacienteByAuthSub = pacienteRepository.findByPatientAuthSub(resolvedAuth0UserId);
			if (pacienteByAuthSub.isPresent()) {
				final Paciente paciente = pacienteByAuthSub.get();
				backfillAppleSubjectIfMissing(paciente, normalizedSubject);
				return AppleIdentityMappingResult.mapped(resolvedAuth0UserId, paciente.getId());
			}
			return AppleIdentityMappingResult.noLocalUser(resolvedAuth0UserId);
		}
		catch (Auth0ManagementApiException ex) {
			if (log.isWarnEnabled()) {
				log.warn("Auth0 lookup failed for Apple subject hash={}", normalizedSubject.hashCode());
			}
			return AppleIdentityMappingResult.auth0LookupFailed(ex.getMessage());
		}
	}

	private Auth0Resolution resolveAuth0UserId(final String appleSubject, final String email) {
		if (auth0ManagementUserService.isConfigured()) {
			final Optional<Auth0ManagementUser> auth0User = auth0ManagementUserService
				.findUserByAppleSubject(appleSubject);
			if (auth0User.isPresent()) {
				return Auth0Resolution.found(auth0User.get().userId());
			}
			final Auth0Resolution emailResolution = resolveAuth0UserIdByEmail(email);
			if (emailResolution != null) {
				return emailResolution;
			}
		}
		final String canonicalAuth0UserId = Auth0AppleIdentitySupport.toAuth0UserId(appleSubject);
		final Optional<Paciente> localByCanonical = pacienteRepository.findByPatientAuthSub(canonicalAuth0UserId);
		if (localByCanonical.isPresent()) {
			return Auth0Resolution.found(canonicalAuth0UserId);
		}
		return Auth0Resolution.notFound();
	}

	private Auth0Resolution resolveAuth0UserIdByEmail(final String email) {
		if (!StringUtils.hasText(email)) {
			return null;
		}
		final List<Auth0ManagementUser> emailMatches = auth0ManagementUserService.searchUsersByEmail(email.trim());
		final List<Auth0ManagementUser> appleEmailMatches = emailMatches.stream()
			.filter(user -> Auth0AppleIdentitySupport.isAppleAuth0UserId(user.userId()))
			.toList();
		if (appleEmailMatches.size() == 1) {
			return Auth0Resolution.found(appleEmailMatches.get(0).userId());
		}
		if (appleEmailMatches.size() > 1) {
			return Auth0Resolution.failed(AppleIdentityMappingStatus.AMBIGUOUS,
					"Multiple Auth0 Apple users matched relay email");
		}
		return null;
	}

	private void backfillAppleSubjectIfMissing(final Paciente paciente, final String appleSubject) {
		if (!StringUtils.hasText(paciente.getAppleSubject())) {
			paciente.setAppleSubject(appleSubject);
			pacienteRepository.save(paciente);
			if (log.isDebugEnabled()) {
				log.debug("Backfilled Apple subject for pacienteId={}", paciente.getId());
			}
		}
	}

	private record Auth0Resolution(Optional<String> userId, AppleIdentityMappingStatus failureStatus, String detail) {

		private static Auth0Resolution found(final String userId) {
			return new Auth0Resolution(Optional.of(userId), null, null);
		}

		private static Auth0Resolution notFound() {
			return new Auth0Resolution(Optional.empty(), null, null);
		}

		private static Auth0Resolution failed(final AppleIdentityMappingStatus status, final String detail) {
			return new Auth0Resolution(Optional.empty(), status, detail);
		}

	}

}
