package com.nutriconsultas.subscription.invitation;

import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.nutriconsultas.subscription.PlanTier;

/**
 * Renders nutritionist invitation HTML from the shared Thymeleaf template.
 */
@Component
public class InvitationEmailTemplateRenderer {

	private static final String NUTRITIONIST_TEMPLATE = "email/nutritionist-invitation";

	private static final String CLINIC_TEMPLATE = "email/clinic-invitation";

	private static final String INVITATION_SUBJECT = "Invitación a Minutriporción";

	private static final String CLINIC_INVITATION_SUBJECT = "Invitación al consultorio en Minutriporción";

	private final SpringTemplateEngine templateEngine;

	public InvitationEmailTemplateRenderer(final SpringTemplateEngine templateEngine) {
		this.templateEngine = templateEngine;
	}

	public String renderHtmlBody(final PlanTier planTier, final String inviteUrl) {
		final Context context = new Context();
		context.setVariable("planTier", planTier);
		context.setVariable("inviteUrl", inviteUrl);
		return templateEngine.process(NUTRITIONIST_TEMPLATE, context);
	}

	public String renderClinicHtmlBody(final String clinicName, final String inviteUrl) {
		final Context context = new Context();
		context.setVariable("clinicName", clinicName);
		context.setVariable("inviteUrl", inviteUrl);
		return templateEngine.process(CLINIC_TEMPLATE, context);
	}

	public String subject() {
		return INVITATION_SUBJECT;
	}

	public String clinicSubject() {
		return CLINIC_INVITATION_SUBJECT;
	}

}
