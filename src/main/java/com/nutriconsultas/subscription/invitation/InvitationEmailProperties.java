package com.nutriconsultas.subscription.invitation;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for nutritionist invitation email delivery (#209).
 */
@ConfigurationProperties(prefix = "nutriconsultas.subscription.invitation.email")
public class InvitationEmailProperties {

	private String mode = "console";

	private String fromAddress = "";

	private String sesRegion = "us-east-1";

	public String getMode() {
		return mode;
	}

	public void setMode(final String mode) {
		this.mode = mode;
	}

	public String getFromAddress() {
		return fromAddress;
	}

	public void setFromAddress(final String fromAddress) {
		this.fromAddress = fromAddress;
	}

	public String getSesRegion() {
		return sesRegion;
	}

	public void setSesRegion(final String sesRegion) {
		this.sesRegion = sesRegion;
	}

}
