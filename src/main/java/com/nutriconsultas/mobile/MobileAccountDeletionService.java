package com.nutriconsultas.mobile;

import java.util.Optional;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nutriconsultas.auth0.Auth0ManagementApiException;
import com.nutriconsultas.auth0.Auth0ManagementUserService;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteDeletionService;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

/**
 * Permanently deletes the signed-in patient account. This is not a deactivation: the
 * Auth0 user is removed, and a linked patient row is removed with its in-app history (App
 * Store Guideline 5.1.1(v)).
 */
@Service
@Slf4j
public class MobileAccountDeletionService {

	private final PacienteRepository pacienteRepository;

	private final PacienteDeletionService pacienteDeletionService;

	private final Auth0ManagementUserService auth0ManagementUserService;

	public MobileAccountDeletionService(final PacienteRepository pacienteRepository,
			final PacienteDeletionService pacienteDeletionService,
			final Auth0ManagementUserService auth0ManagementUserService) {
		this.pacienteRepository = pacienteRepository;
		this.pacienteDeletionService = pacienteDeletionService;
		this.auth0ManagementUserService = auth0ManagementUserService;
	}

	/**
	 * Deletes local patient data first, then the Auth0 user. Auth0 failure rolls the
	 * local delete back so a failed attempt leaves the account intact.
	 * @param jwt authenticated mobile token; {@code sub} is the Auth0 user id
	 */
	@Transactional
	public void deleteAuthenticatedAccount(final Jwt jwt) {
		if (jwt == null || !StringUtils.hasText(jwt.getSubject())) {
			throw new AccountDeletionUnavailableException();
		}
		final String subject = jwt.getSubject();
		final Optional<Paciente> patient = pacienteRepository.findByPatientAuthSub(subject);
		patient.ifPresent(this::deleteLinkedPatient);
		deleteAuth0User(subject);
		if (log.isInfoEnabled()) {
			log.info("Deleted mobile account sub={}", LogRedaction.redactUserId(subject));
		}
	}

	private void deleteLinkedPatient(final Paciente paciente) {
		if (paciente.getId() == null || !StringUtils.hasText(paciente.getUserId())) {
			throw new AccountDeletionUnavailableException();
		}
		pacienteDeletionService.deletePatientWithHistory(paciente.getId(), paciente.getUserId());
	}

	private void deleteAuth0User(final String subject) {
		try {
			auth0ManagementUserService.deleteUserForAccountClosure(subject);
		}
		catch (Auth0ManagementApiException | IllegalStateException ex) {
			throw new AccountDeletionUnavailableException(ex);
		}
	}

}
