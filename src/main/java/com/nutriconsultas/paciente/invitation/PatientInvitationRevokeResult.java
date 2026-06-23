package com.nutriconsultas.paciente.invitation;

import com.nutriconsultas.paciente.PatientInvitationStatus;

/**
 * Result after a nutritionist revokes a patient invitation (#139).
 */
public record PatientInvitationRevokeResult(Long invitationId, Long pacienteId, PatientInvitationStatus status) {
}
