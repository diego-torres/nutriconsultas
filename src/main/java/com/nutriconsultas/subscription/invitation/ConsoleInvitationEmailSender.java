package com.nutriconsultas.subscription.invitation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.nutriconsultas.subscription.PlanTier;
import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

/**
 * Local/dev sender: logs a grep-friendly invite link without SMTP or AWS (#209).
 */
@Component
@ConditionalOnProperty(prefix = "nutriconsultas.subscription.invitation.email", name = "mode", havingValue = "console",
		matchIfMissing = true)
@Slf4j
public class ConsoleInvitationEmailSender implements InvitationEmailSender {

	private final InvitationEmailTemplateRenderer templateRenderer;

	public ConsoleInvitationEmailSender(final InvitationEmailTemplateRenderer templateRenderer) {
		this.templateRenderer = templateRenderer;
	}

	@Override
	public void sendNutritionistInvitation(final String recipientEmail, final PlanTier planTier,
			final String inviteUrl) {
		final String body = templateRenderer.renderHtmlBody(planTier, inviteUrl);
		if (log.isInfoEnabled()) {
			log.info("INVITATION_LINK={}", inviteUrl);
			log.info("Nutritionist invitation email (console mode): planTier={}, recipient={}, bodyLength={}", planTier,
					LogRedaction.redactEmail(recipientEmail), body.length());
		}
	}

	@Override
	public void sendClinicInvitation(final String recipientEmail, final String clinicName, final String inviteUrl) {
		final String body = templateRenderer.renderClinicHtmlBody(clinicName, inviteUrl);
		if (log.isInfoEnabled()) {
			log.info("CLINIC_INVITATION_LINK={}", inviteUrl);
			log.info("Clinic invitation email (console mode): clinicName={}, recipient={}, bodyLength={}", clinicName,
					LogRedaction.redactEmail(recipientEmail), body.length());
		}
	}

}
