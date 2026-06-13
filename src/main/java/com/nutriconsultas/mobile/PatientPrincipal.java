package com.nutriconsultas.mobile;

import java.io.Serial;
import java.io.Serializable;

/**
 * Authenticated patient identity for mobile API requests. Contains only non-PHI
 * identifiers.
 */
public final class PatientPrincipal implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private final Long pacienteId;

	private final String patientAuthSub;

	public PatientPrincipal(final Long pacienteId, final String patientAuthSub) {
		this.pacienteId = pacienteId;
		this.patientAuthSub = patientAuthSub;
	}

	public Long getPacienteId() {
		return pacienteId;
	}

	public String getPatientAuthSub() {
		return patientAuthSub;
	}

}
