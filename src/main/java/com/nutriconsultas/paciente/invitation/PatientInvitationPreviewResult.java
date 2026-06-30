package com.nutriconsultas.paciente.invitation;

import com.nutriconsultas.paciente.PacienteStatus;

/**
 * Public invitation preview payload (#135, #349). Inviter branding plus non-PII auth
 * routing hints — no full email or patient name.
 */
public record PatientInvitationPreviewResult(String inviterDisplayName, PacienteStatus patientStatus,
		boolean mobileAppLinked, InvitationAuthPath authPath, String emailHint) {

}
