package com.nutriconsultas.paciente.invitation;

import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * Renders patient onboarding invitation HTML (#134).
 */
@Component
public class PatientInvitationEmailTemplateRenderer {

	private static final String TEMPLATE = "email/patient-invitation";

	static final String SUBJECT = "Invitación a la app Minutriporción";

	private final SpringTemplateEngine templateEngine;

	public PatientInvitationEmailTemplateRenderer(final SpringTemplateEngine templateEngine) {
		this.templateEngine = templateEngine;
	}

	public String renderHtmlBody(final String humanCode, final String inviteUrl) {
		final Context context = new Context();
		context.setVariable("humanCode", humanCode);
		context.setVariable("inviteUrl", inviteUrl);
		return templateEngine.process(TEMPLATE, context);
	}

}
