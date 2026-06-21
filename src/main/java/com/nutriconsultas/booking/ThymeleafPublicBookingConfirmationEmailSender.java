package com.nutriconsultas.booking;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import lombok.extern.slf4j.Slf4j;

/**
 * Renders public booking confirmation email. Delivery is logged until SMTP is configured
 * (#209).
 */
@Component
@Slf4j
public class ThymeleafPublicBookingConfirmationEmailSender implements PublicBookingConfirmationEmailSender {

	private static final String TEMPLATE = "email/public-booking-confirmation";

	private final SpringTemplateEngine templateEngine;

	public ThymeleafPublicBookingConfirmationEmailSender(final SpringTemplateEngine templateEngine) {
		this.templateEngine = templateEngine;
	}

	@Override
	public void sendConfirmation(final String recipientEmail, final PublicBookingConfirmationEmailDetails details) {
		if (!StringUtils.hasText(recipientEmail) || details == null) {
			return;
		}
		final Context context = new Context();
		context.setVariable("patientName", details.patientName());
		context.setVariable("nutritionistDisplayName", details.nutritionistDisplayName());
		context.setVariable("appointmentDateFormatted", details.appointmentDateFormatted());
		context.setVariable("appointmentTimeFormatted", details.appointmentTimeFormatted());
		final String body = templateEngine.process(TEMPLATE, context);
		if (log.isInfoEnabled()) {
			log.info("Public booking confirmation email prepared: eventContext=public-booking, bodyLength={}",
					body.length());
		}
	}

}
