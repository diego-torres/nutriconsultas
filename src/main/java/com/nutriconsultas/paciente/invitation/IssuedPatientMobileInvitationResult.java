package com.nutriconsultas.paciente.invitation;

import java.time.Instant;

/**
 * Web-safe result after sending a patient mobile invitation (#341). Does not include raw
 * URL token.
 */
public record IssuedPatientMobileInvitationResult(Long invitationId, Long pacienteId, String humanCode,
		Instant expiresAt, String recipientEmailRedacted) {
}
