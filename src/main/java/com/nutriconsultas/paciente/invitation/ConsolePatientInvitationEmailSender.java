package com.nutriconsultas.paciente.invitation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

/**
 * Local/dev sender for patient invitations (#134). Does not log invite URLs or human
 * codes.
 */
@Component
@ConditionalOnProperty(prefix = "nutriconsultas.subscription.invitation.email", name = "mode", havingValue = "console",
		matchIfMissing = true)
@Slf4j
public class ConsolePatientInvitationEmailSender implements PatientInvitationEmailSender {

	private final PatientInvitationEmailTemplateRenderer templateRenderer;

	public ConsolePatientInvitationEmailSender(final PatientInvitationEmailTemplateRenderer templateRenderer) {
		this.templateRenderer = templateRenderer;
	}

	@Override
	public void sendPatientInvitation(final String recipientEmail, final String humanCode, final String inviteUrl) {
		final String body = templateRenderer.renderHtmlBody(humanCode, inviteUrl);
		if (log.isInfoEnabled()) {
			log.info("Patient invitation email queued (console mode): recipient={}, bodyLength={}",
					LogRedaction.redactEmail(recipientEmail), body.length());
		}
	}

}
