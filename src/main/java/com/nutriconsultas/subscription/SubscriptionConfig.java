package com.nutriconsultas.subscription;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.nutriconsultas.subscription.invitation.NutritionistInvitationProperties;
import com.nutriconsultas.subscription.payment.PaymentProperties;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({ SubscriptionProperties.class, PaymentProperties.class,
		NutritionistInvitationProperties.class })
public class SubscriptionConfig {

}
