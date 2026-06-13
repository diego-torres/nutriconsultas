package com.nutriconsultas.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

@Configuration
@Profile("test")
public class TestMobileJwtDecoderConfig {

	public static final String TOKEN_PREFIX = "test-jwt:";

	@Bean
	JwtDecoder mobileJwtDecoder(
			@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") final String issuerUri,
			@Value("${app.security.jwt.audience}") final String expectedAudience) {
		return token -> {
			if (token == null || !token.startsWith(TOKEN_PREFIX)) {
				throw new JwtException("Unsupported test JWT token");
			}
			final String subject = token.substring(TOKEN_PREFIX.length());
			return Jwt.withTokenValue(token)
				.header("alg", "none")
				.subject(subject)
				.issuer(issuerUri)
				.claim("aud", List.of(expectedAudience))
				.build();
		};
	}

}
