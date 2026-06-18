package com.nutriconsultas.paciente.invitation;

/**
 * One-time invitation credentials returned to the issuer (#133). The raw {@code urlToken}
 * must never be logged or persisted — only {@link #tokenHash()} is stored.
 */
public record PatientInvitationTokenBundle(String urlToken, String humanCode, String tokenHash) {

}
