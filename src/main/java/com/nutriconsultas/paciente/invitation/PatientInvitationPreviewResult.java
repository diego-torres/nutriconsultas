package com.nutriconsultas.paciente.invitation;

/**
 * Public invitation preview payload (#135). Contains inviter branding only — no patient
 * PII.
 */
public record PatientInvitationPreviewResult(String inviterDisplayName) {

}
