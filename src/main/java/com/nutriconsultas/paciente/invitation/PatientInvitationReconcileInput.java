package com.nutriconsultas.paciente.invitation;

/**
 * Parameters for post-login invitation reconciliation (#136).
 */
public record PatientInvitationReconcileInput(String patientAuthSub, String email, String rawUrlToken,
		String humanCode) {

}
