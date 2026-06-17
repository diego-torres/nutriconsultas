package com.nutriconsultas.subscription.invitation;

import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.nutriconsultas.subscription.PlanTier;

import lombok.extern.slf4j.Slf4j;

/**
 * Renders invitation email templates. Delivery is logged until SMTP is configured.
 */
@Component
@Slf4j
public class ThymeleafInvitationEmailSender implements InvitationEmailSender {

	private final SpringTemplateEngine templateEngine;

	public ThymeleafInvitationEmailSender(final SpringTemplateEngine templateEngine) {
		this.templateEngine = templateEngine;
	}

	@Override
	public void sendNutritionistInvitation(final String recipientEmail, final PlanTier planTier,
			final String inviteUrl) {
		final Context context = new Context();
		context.setVariable("planTier", planTier);
		context.setVariable("inviteUrl", inviteUrl);
		final String body = templateEngine.process("email/nutritionist-invitation", context);
		if (log.isInfoEnabled()) {
			log.info("Nutritionist invitation email prepared: planTier={}, bodyLength={}", planTier, body.length());
		}
	}

}
