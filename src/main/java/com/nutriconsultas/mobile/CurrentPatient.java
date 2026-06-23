package com.nutriconsultas.mobile;

import com.nutriconsultas.paciente.PacienteStatus;
import com.nutriconsultas.paciente.projection.PacienteAuthView;

/**
 * Resolved mobile patient identity for JWT {@code sub} → {@code patientAuthSub} (#137).
 */
public record CurrentPatient(Long pacienteId, String patientAuthSub, PacienteStatus status) {

	public static CurrentPatient from(final PacienteAuthView authView) {
		return new CurrentPatient(authView.getId(), authView.getPatientAuthSub(), authView.getStatus());
	}

	public PatientPrincipal toPrincipal() {
		return new PatientPrincipal(pacienteId, patientAuthSub, status);
	}

}
