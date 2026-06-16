package com.nutriconsultas.mobile;

import org.springframework.security.oauth2.jwt.Jwt;

import com.nutriconsultas.paciente.projection.PacienteAuthView;

/**
 * Base controller for patient mobile endpoints. Resolves the authenticated patient from
 * JWT {@code sub} → {@code Paciente.patientAuthSub} using a lightweight auth projection
 * (#156 Phase A).
 */
public abstract class AbstractMobilePatientController {

	private final PatientAuthService patientAuthService;

	protected AbstractMobilePatientController(final PatientAuthService patientAuthService) {
		this.patientAuthService = patientAuthService;
	}

	protected PacienteAuthView getAuthenticatedPacienteAuthView(final Jwt jwt) {
		if (jwt == null) {
			throw new PatientNotLinkedException();
		}
		return patientAuthService.requireAuthViewByJwt(jwt);
	}

	protected Long getAuthenticatedPacienteId(final Jwt jwt) {
		return getAuthenticatedPacienteAuthView(jwt).getId();
	}

}
