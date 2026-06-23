package com.nutriconsultas.paciente.invitation;

import java.time.Instant;

import com.nutriconsultas.paciente.PacienteStatus;

/**
 * Service-layer result for patient invitation redemption (#136).
 */
public record PatientInvitationRedeemResult(Long pacienteId, PacienteStatus pacienteStatus, Long invitationId,
		Instant redeemedAt) {

}
