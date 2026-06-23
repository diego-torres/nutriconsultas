package com.nutriconsultas.mobile;

import java.io.Serial;
import java.io.Serializable;

import com.nutriconsultas.paciente.PacienteStatus;

/**
 * Authenticated patient identity for mobile API requests. Contains only non-PHI
 * identifiers.
 */
public final class PatientPrincipal implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private final Long pacienteId;

	private final String patientAuthSub;

	private final PacienteStatus status;

	public PatientPrincipal(final Long pacienteId, final String patientAuthSub, final PacienteStatus status) {
		this.pacienteId = pacienteId;
		this.patientAuthSub = patientAuthSub;
		this.status = status;
	}

	public Long getPacienteId() {
		return pacienteId;
	}

	public String getPatientAuthSub() {
		return patientAuthSub;
	}

	public PacienteStatus getStatus() {
		return status;
	}

}
