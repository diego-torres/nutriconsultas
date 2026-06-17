package com.nutriconsultas.subscription.lifecycle;

import com.nutriconsultas.subscription.Subscription;
import com.nutriconsultas.subscription.SubscriptionStatus;

public record SubscriptionBanner(SubscriptionStatus status, java.time.Instant periodEnd, int gracePeriodDays,
		String message, String alertClass) {

	public static SubscriptionBanner none() {
		return null;
	}

	public static SubscriptionBanner forSubscription(final Subscription subscription) {
		if (subscription == null) {
			return none();
		}
		return switch (subscription.getStatus()) {
			case GRACE -> new SubscriptionBanner(subscription.getStatus(), subscription.getPeriodEnd(),
					subscription.getGracePeriodDays(),
					"Su suscripción está en periodo de gracia. Renueve el pago para evitar la suspensión del acceso.",
					"warning");
			case ACTIVE -> {
				if (subscription.getPeriodEnd() == null) {
					yield none();
				}
				yield new SubscriptionBanner(subscription.getStatus(), subscription.getPeriodEnd(),
						subscription.getGracePeriodDays(),
						"Su suscripción vence pronto. Renueve el pago para mantener el acceso completo.", "info");
			}
			case SUSPENDED -> new SubscriptionBanner(subscription.getStatus(), subscription.getPeriodEnd(),
					subscription.getGracePeriodDays(),
					"Su suscripción está suspendida. Contacte a soporte o renueve el pago para recuperar el acceso.",
					"danger");
			default -> none();
		};
	}

}
