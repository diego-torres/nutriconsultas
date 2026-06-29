package com.nutriconsultas.auth0;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Confidential Auth0 client used server-side to broker patient database signup/login.
 * Password grant must be enabled on this application only — not on the public Native app.
 */
@ConfigurationProperties(prefix = "app.auth0.patient-broker")
@Getter
@Setter
public class Auth0PatientAuthenticationProperties {

	private String domain = "";

	/** Native application client id — used for {@code /dbconnections/signup} only. */
	private String nativeClientId = "";

	private String brokerClientId = "";

	private String brokerClientSecret = "";

	private String databaseConnection = "Username-Password-Authentication";

	private String audience = "";

}
