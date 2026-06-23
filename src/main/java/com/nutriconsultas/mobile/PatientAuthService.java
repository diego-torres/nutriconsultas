package com.nutriconsultas.mobile;

import java.util.Optional;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.nutriconsultas.paciente.projection.PacienteAuthView;

/**
 * Facade for mobile patient JWT resolution; delegates to {@link CurrentPatientService}
 * (#137).
 */
@Service
public class PatientAuthService {

	private final CurrentPatientService currentPatientService;

	public PatientAuthService(final CurrentPatientService currentPatientService) {
		this.currentPatientService = currentPatientService;
	}

	public Optional<PacienteAuthView> findAuthViewByJwt(final Jwt jwt) {
		return currentPatientService.findAuthViewByJwt(jwt);
	}

	public PacienteAuthView requireAuthViewByJwt(final Jwt jwt) {
		return findAuthViewByJwt(jwt).orElseThrow(PatientNotLinkedException::new);
	}

	public PatientPrincipal resolvePrincipal(final Jwt jwt) {
		return currentPatientService.resolvePrincipal(jwt);
	}

}
