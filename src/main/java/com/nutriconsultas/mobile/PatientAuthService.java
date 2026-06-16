package com.nutriconsultas.mobile;

import java.util.Optional;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.projection.PacienteAuthView;
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
	public Optional<PacienteAuthView> findAuthViewByJwt(final Jwt jwt) {
		if (jwt == null || jwt.getSubject() == null) {
			return Optional.empty();
		}
		return pacienteRepository.findAuthViewByPatientAuthSub(jwt.getSubject());
	}

	@Transactional(readOnly = true)
	public PacienteAuthView requireAuthViewByJwt(final Jwt jwt) {
		return findAuthViewByJwt(jwt).orElseThrow(PatientNotLinkedException::new);
	}

	@Transactional(readOnly = true)
	public PatientPrincipal resolvePrincipal(final Jwt jwt) {
		final PacienteAuthView authView = requireAuthViewByJwt(jwt);
		if (log.isDebugEnabled()) {
			log.debug("Resolved mobile patient principal: {}", LogRedaction.redactPaciente(authView.getId()));
		}
		return new PatientPrincipal(authView.getId(), jwt.getSubject());
	}

}
