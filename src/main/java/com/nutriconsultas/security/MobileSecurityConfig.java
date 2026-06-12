package com.nutriconsultas.security;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import com.nutriconsultas.mobile.PatientLinkageFilter;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class MobileSecurityConfig {

	private final PatientLinkageFilter patientLinkageFilter;

	private final JwtDecoder mobileJwtDecoder;

	public MobileSecurityConfig(final PatientLinkageFilter patientLinkageFilter, final JwtDecoder mobileJwtDecoder) {
		this.patientLinkageFilter = patientLinkageFilter;
		this.mobileJwtDecoder = mobileJwtDecoder;
	}

	@Bean
	@Order(1)
	SecurityFilterChain mobileFilterChain(final HttpSecurity http) throws Exception {
		log.info("ENABLING mobile JWT security filter chain for /rest/mobile/**");

		http.securityMatcher("/rest/mobile/**")
			.cors(withDefaults())
			.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(mobileJwtDecoder)))
			.addFilterAfter(patientLinkageFilter, BearerTokenAuthenticationFilter.class);

		return http.build();
	}

}
