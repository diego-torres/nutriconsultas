package com.nutriconsultas.subscription.invitation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.nutriconsultas.subscription.PlanTier;
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
 * Sends nutritionist invitation emails via Amazon SES (#209).
 */
@Component
@ConditionalOnProperty(prefix = "nutriconsultas.subscription.invitation.email", name = "mode", havingValue = "ses")
@Slf4j
public class SesInvitationEmailSender implements InvitationEmailSender {

	private final InvitationEmailProperties invitationEmailProperties;

	private final InvitationEmailTemplateRenderer templateRenderer;

	private final SesV2Client sesV2Client;

	public SesInvitationEmailSender(final InvitationEmailProperties invitationEmailProperties,
			final InvitationEmailTemplateRenderer templateRenderer, final SesV2Client sesV2Client) {
		this.invitationEmailProperties = invitationEmailProperties;
		this.templateRenderer = templateRenderer;
		this.sesV2Client = sesV2Client;
		if (log.isInfoEnabled()) {
			log.info("SES invitation email sender active (region={})", invitationEmailProperties.getSesRegion());
		}
	}

	@Override
	public void sendNutritionistInvitation(final String recipientEmail, final PlanTier planTier,
			final String inviteUrl) {
		final String fromAddress = invitationEmailProperties.getFromAddress();
		if (!StringUtils.hasText(fromAddress)) {
			throw new IllegalStateException(
					"MAIL_FROM / nutriconsultas.subscription.invitation.email.from-address is required in ses mode");
		}
		final String htmlBody = templateRenderer.renderHtmlBody(planTier, inviteUrl);
		final SendEmailRequest request = SendEmailRequest.builder()
			.fromEmailAddress(fromAddress)
			.destination(Destination.builder().toAddresses(recipientEmail).build())
			.content(EmailContent.builder()
				.simple(Message.builder()
					.subject(Content.builder().data(templateRenderer.subject()).charset("UTF-8").build())
					.body(Body.builder().html(Content.builder().data(htmlBody).charset("UTF-8").build()).build())
					.build())
				.build())
			.build();
		try {
			sesV2Client.sendEmail(request);
			if (log.isInfoEnabled()) {
				log.info("Nutritionist invitation email sent via SES: planTier={}, recipient={}", planTier,
						LogRedaction.redactEmail(recipientEmail));
			}
		}
		catch (SesV2Exception ex) {
			log.error("Failed to send nutritionist invitation email via SES: planTier={}, recipient={}", planTier,
					LogRedaction.redactEmail(recipientEmail), ex);
			throw ex;
		}
	}

	@Override
	public void sendClinicInvitation(final String recipientEmail, final String clinicName, final String inviteUrl) {
		final String fromAddress = invitationEmailProperties.getFromAddress();
		if (!StringUtils.hasText(fromAddress)) {
			throw new IllegalStateException(
					"MAIL_FROM / nutriconsultas.subscription.invitation.email.from-address is required in ses mode");
		}
		final String htmlBody = templateRenderer.renderClinicHtmlBody(clinicName, inviteUrl);
		final SendEmailRequest request = SendEmailRequest.builder()
			.fromEmailAddress(fromAddress)
			.destination(Destination.builder().toAddresses(recipientEmail).build())
			.content(EmailContent.builder()
				.simple(Message.builder()
					.subject(Content.builder().data(templateRenderer.clinicSubject()).charset("UTF-8").build())
					.body(Body.builder().html(Content.builder().data(htmlBody).charset("UTF-8").build()).build())
					.build())
				.build())
			.build();
		try {
			sesV2Client.sendEmail(request);
			if (log.isInfoEnabled()) {
				log.info("Clinic invitation email sent via SES: clinicName={}, recipient={}", clinicName,
						LogRedaction.redactEmail(recipientEmail));
			}
		}
		catch (SesV2Exception ex) {
			log.error("Failed to send clinic invitation email via SES: clinicName={}, recipient={}", clinicName,
					LogRedaction.redactEmail(recipientEmail), ex);
			throw ex;
		}
	}

}
