package com.nutriconsultas.paciente.invitation;

import java.time.Instant;

/**
 * Service-layer result for nutritionist-created patient invitations (#134).
 */
public record CreatedPatientInvitationResult(Long invitationId, Long pacienteId, String inviteUrl, String humanCode,
		Instant expiresAt, String offlineJws) {
}
