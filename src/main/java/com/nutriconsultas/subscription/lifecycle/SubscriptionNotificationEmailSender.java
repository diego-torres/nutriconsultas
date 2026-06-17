package com.nutriconsultas.subscription.lifecycle;

import com.nutriconsultas.subscription.Subscription;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface SubscriptionNotificationEmailSender {

	void sendNotification(String recipientEmail, Subscription subscription,
			SubscriptionNotificationType notificationType);

}
