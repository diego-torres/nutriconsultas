package com.nutriconsultas.mobile;

import org.springframework.security.oauth2.jwt.Jwt;

import com.nutriconsultas.paciente.Paciente;

/**
 * Base controller for patient mobile endpoints. Resolves the authenticated
 * {@link Paciente} from JWT {@code sub} → {@code Paciente.patientAuthSub}.
 */
public abstract class AbstractMobilePatientController {

	private final PatientAuthService patientAuthService;

	protected AbstractMobilePatientController(final PatientAuthService patientAuthService) {
		this.patientAuthService = patientAuthService;
	}

	protected Paciente getAuthenticatedPaciente(final Jwt jwt) {
		if (jwt == null) {
			throw new PatientNotLinkedException();
		}
		return patientAuthService.requirePacienteByJwt(jwt);
	}

}
