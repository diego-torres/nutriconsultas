package com.nutriconsultas.mobile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

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

	protected Paciente getAuthenticatedPaciente(@AuthenticationPrincipal final JwtAuthenticationToken authentication) {
		if (authentication == null) {
			throw new PatientNotLinkedException();
		}
		if (authentication.getDetails() instanceof PatientPrincipal patientPrincipal) {
			return patientAuthService.requirePacienteById(patientPrincipal.getPacienteId());
		}
		return patientAuthService.requirePacienteByJwt(authentication.getToken());
	}

}
