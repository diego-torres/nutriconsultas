package com.nutriconsultas.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class MobileJwtDecoderConfig {

	@Bean
	JwtDecoder mobileJwtDecoder(
			@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") final String issuerUri,
			@Value("${app.security.jwt.audience}") final String expectedAudience) {
		final NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
		final OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>("aud",
				aud -> aud != null && aud.contains(expectedAudience));
		final OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
				JwtValidators.createDefaultWithIssuer(issuerUri), audienceValidator);
		decoder.setJwtValidator(validator);
		return decoder;
	}

}
