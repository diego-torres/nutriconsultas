package com.nutriconsultas.subscription.invitation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;

/**
 * AWS SES v2 client for production invitation email delivery (#209).
 */
@Configuration
@ConditionalOnProperty(prefix = "nutriconsultas.subscription.invitation.email", name = "mode", havingValue = "ses")
public class SesInvitationEmailConfiguration {

	@Bean
	public SesV2Client sesV2Client(final InvitationEmailProperties invitationEmailProperties) {
		return SesV2Client.builder()
			.region(Region.of(invitationEmailProperties.getSesRegion()))
			.credentialsProvider(DefaultCredentialsProvider.create())
			.build();
	}

}
