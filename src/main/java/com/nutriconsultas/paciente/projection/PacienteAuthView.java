package com.nutriconsultas.paciente.projection;

import com.nutriconsultas.paciente.ApplePacienteLifecycleStatus;
import com.nutriconsultas.paciente.PacienteStatus;

/**
 * Read-only projection for mobile JWT resolution ({@code sub} → {@code patientAuthSub}) —
 * #156 Phase A; status added for onboarding gate (#137).
 */
public interface PacienteAuthView {

	Long getId();

	String getPatientAuthSub();

	String getUserId();

	PacienteStatus getStatus();

	ApplePacienteLifecycleStatus getAppleLifecycleStatus();

}
