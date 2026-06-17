package com.nutriconsultas.paciente;

/**
 * Invite-only patient onboarding lifecycle (#132). Existing patients default to
 * {@link #ACTIVE}.
 */
public enum PacienteStatus {

	INVITED, ONBOARDING, ACTIVE, REVOKED

}
