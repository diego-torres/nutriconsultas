package com.nutriconsultas.subscription.clinic;

import java.time.Instant;

/**
 * Pending clinic invitation row for the director roster UI.
 */
public record ClinicInvitationView(Long invitationId, String email, Instant expiresAt, Instant createdAt) {
}
