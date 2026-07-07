package com.nutriconsultas.paciente;

/**
 * Apple Sign-In lifecycle state for a patient account (#506). Distinct from
 * {@link PacienteStatus} invitation/onboarding flow.
 */
public enum ApplePacienteLifecycleStatus {

	NONE, ACCESS_REVOKED, PENDING_DELETION_REVIEW

}
