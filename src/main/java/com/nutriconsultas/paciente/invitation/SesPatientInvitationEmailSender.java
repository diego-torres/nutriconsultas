package com.nutriconsultas.paciente.invitation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.nutriconsultas.subscription.invitation.InvitationEmailProperties;
import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;

/**
 * Sends patient onboarding invitation emails via Amazon SES (#134).
 */
@Component
@ConditionalOnProperty(prefix = "nutriconsultas.subscription.invitation.email", name = "mode", havingValue = "ses")
@Slf4j
public class SesPatientInvitationEmailSender implements PatientInvitationEmailSender {

	private final InvitationEmailProperties invitationEmailProperties;

	private final PatientInvitationEmailTemplateRenderer templateRenderer;

	private final SesV2Client sesV2Client;

	public SesPatientInvitationEmailSender(final InvitationEmailProperties invitationEmailProperties,
			final PatientInvitationEmailTemplateRenderer templateRenderer, final SesV2Client sesV2Client) {
		this.invitationEmailProperties = invitationEmailProperties;
		this.templateRenderer = templateRenderer;
		this.sesV2Client = sesV2Client;
	}

	@Override
	public void sendPatientInvitation(final String recipientEmail, final String humanCode, final String inviteUrl) {
		final String fromAddress = invitationEmailProperties.getFromAddress();
		if (!StringUtils.hasText(fromAddress)) {
			throw new IllegalStateException(
					"MAIL_FROM / nutriconsultas.subscription.invitation.email.from-address is required in ses mode");
		}
		final String htmlBody = templateRenderer.renderHtmlBody(humanCode, inviteUrl);
		final SendEmailRequest request = SendEmailRequest.builder()
			.fromEmailAddress(fromAddress)
			.destination(Destination.builder().toAddresses(recipientEmail).build())
			.content(EmailContent.builder()
				.simple(Message.builder()
					.subject(Content.builder()
						.data(PatientInvitationEmailTemplateRenderer.SUBJECT)
						.charset("UTF-8")
						.build())
					.body(Body.builder().html(Content.builder().data(htmlBody).charset("UTF-8").build()).build())
					.build())
				.build())
			.build();
		try {
			sesV2Client.sendEmail(request);
			if (log.isInfoEnabled()) {
				log.info("Patient invitation email sent via SES: recipient={}",
						LogRedaction.redactEmail(recipientEmail));
			}
		}
		catch (SesV2Exception ex) {
			log.error("Failed to send patient invitation email via SES: recipient={}",
					LogRedaction.redactEmail(recipientEmail), ex);
			throw ex;
		}
	}

}
