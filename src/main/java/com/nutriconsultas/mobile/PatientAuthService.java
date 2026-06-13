package com.nutriconsultas.mobile;

import java.util.Optional;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PatientAuthService {

	private final PacienteRepository pacienteRepository;

	public PatientAuthService(final PacienteRepository pacienteRepository) {
		this.pacienteRepository = pacienteRepository;
	}

	@Transactional(readOnly = true)
	public Optional<Paciente> findByJwt(final Jwt jwt) {
		if (jwt == null || jwt.getSubject() == null) {
			return Optional.empty();
		}
		return pacienteRepository.findByPatientAuthSub(jwt.getSubject());
	}

	@Transactional(readOnly = true)
	public Paciente requirePacienteByJwt(final Jwt jwt) {
		return findByJwt(jwt).orElseThrow(PatientNotLinkedException::new);
	}

	@Transactional(readOnly = true)
	public Paciente requirePacienteById(final Long pacienteId) {
		return pacienteRepository.findById(pacienteId).orElseThrow(PatientNotLinkedException::new);
	}

	@Transactional(readOnly = true)
	public PatientPrincipal resolvePrincipal(final Jwt jwt) {
		final Paciente paciente = requirePacienteByJwt(jwt);
		if (log.isDebugEnabled()) {
			log.debug("Resolved mobile patient principal: {}", LogRedaction.redactPaciente(paciente.getId()));
		}
		return new PatientPrincipal(paciente.getId(), jwt.getSubject());
	}

}
