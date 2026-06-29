package com.nutriconsultas.auth0;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(Auth0PatientAuthenticationProperties.class)
public class Auth0PatientAuthenticationConfig {

}
