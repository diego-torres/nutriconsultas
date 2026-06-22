package com.nutriconsultas.paciente.invitation;

/**
 * Sends patient onboarding invitation emails (#134). Implementations must never log raw
 * tokens or human codes at INFO+.
 */
@FunctionalInterface
public interface PatientInvitationEmailSender {

	void sendPatientInvitation(String recipientEmail, String humanCode, String inviteUrl);

}
