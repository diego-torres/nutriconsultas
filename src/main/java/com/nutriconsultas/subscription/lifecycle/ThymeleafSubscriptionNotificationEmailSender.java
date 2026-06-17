package com.nutriconsultas.subscription.lifecycle;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.nutriconsultas.subscription.Subscription;
import com.nutriconsultas.subscription.SubscriptionStatus;

import lombok.extern.slf4j.Slf4j;

/**
 * Renders subscription lifecycle email templates. Delivery is logged until SMTP is
 * configured (#209).
 */
@Component
@Slf4j
public class ThymeleafSubscriptionNotificationEmailSender implements SubscriptionNotificationEmailSender {

	private static final DateTimeFormatter PERIOD_END_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy")
		.withZone(ZoneId.of("America/Mexico_City"));

	private final SpringTemplateEngine templateEngine;

	public ThymeleafSubscriptionNotificationEmailSender(final SpringTemplateEngine templateEngine) {
		this.templateEngine = templateEngine;
	}

	@Override
	public void sendNotification(final String recipientEmail, final Subscription subscription,
			final SubscriptionNotificationType notificationType) {
		final Context context = new Context();
		context.setVariable("notificationType", notificationType);
		context.setVariable("planTier", subscription.getPlanTier());
		context.setVariable("status", subscription.getStatus());
		context.setVariable("periodEndFormatted", formatPeriodEnd(subscription.getPeriodEnd()));
		context.setVariable("gracePeriodDays", subscription.getGracePeriodDays());
		context.setVariable("subject", subjectFor(notificationType));
		final String body = templateEngine.process("email/subscription-lifecycle", context);
		if (log.isInfoEnabled()) {
			log.info("Subscription notification email prepared: type={}, planTier={}, bodyLength={}", notificationType,
					subscription.getPlanTier(), body.length());
		}
	}

	private static String formatPeriodEnd(final Instant periodEnd) {
		if (periodEnd == null) {
			return "—";
		}
		return PERIOD_END_FORMAT.format(periodEnd);
	}

	private static String subjectFor(final SubscriptionNotificationType notificationType) {
		return switch (notificationType) {
			case EXPIRY_REMINDER_7_DAYS -> "Su suscripción Minutriporción vence en 7 días";
			case EXPIRY_REMINDER_3_DAYS -> "Su suscripción Minutriporción vence en 3 días";
			case EXPIRY_REMINDER_1_DAY -> "Su suscripción Minutriporción vence mañana";
			case GRACE_PERIOD_ENTERED -> "Su suscripción Minutriporción entró en periodo de gracia";
		};
	}

}
