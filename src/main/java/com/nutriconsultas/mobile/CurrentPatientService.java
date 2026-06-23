package com.nutriconsultas.mobile;

import java.util.Optional;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.projection.PacienteAuthView;
import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

/**
 * Central {@code sub → Paciente} resolver for the mobile API (#137).
 */
@Service
@Slf4j
public class CurrentPatientService {

	private final PacienteRepository pacienteRepository;

	public CurrentPatientService(final PacienteRepository pacienteRepository) {
		this.pacienteRepository = pacienteRepository;
	}

	@Transactional(readOnly = true)
	public Optional<CurrentPatient> findByJwt(final Jwt jwt) {
		if (jwt == null || jwt.getSubject() == null) {
			return Optional.empty();
		}
		return pacienteRepository.findAuthViewByPatientAuthSub(jwt.getSubject()).map(CurrentPatient::from);
	}

	@Transactional(readOnly = true)
	public CurrentPatient requireByJwt(final Jwt jwt) {
		return findByJwt(jwt).orElseThrow(PatientNotLinkedException::new);
	}

	@Transactional(readOnly = true)
	public Optional<PacienteAuthView> findAuthViewByJwt(final Jwt jwt) {
		if (jwt == null || jwt.getSubject() == null) {
			return Optional.empty();
		}
		return pacienteRepository.findAuthViewByPatientAuthSub(jwt.getSubject());
	}

	@Transactional(readOnly = true)
	public PatientPrincipal resolvePrincipal(final Jwt jwt) {
		final CurrentPatient currentPatient = requireByJwt(jwt);
		if (log.isDebugEnabled()) {
			log.debug("Resolved mobile current patient: {}", LogRedaction.redactPaciente(currentPatient.pacienteId()));
		}
		return currentPatient.toPrincipal();
	}

}
