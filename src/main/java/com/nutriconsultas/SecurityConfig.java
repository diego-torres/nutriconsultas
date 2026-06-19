package com.nutriconsultas;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

import com.nutriconsultas.admin.LogoutHandler;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {

	private final LogoutHandler logoutHandler;

	public SecurityConfig(final LogoutHandler logoutHandler) {
		this.logoutHandler = logoutHandler;
	}

	@Bean
	@Order(2)
	// package-private
	SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
		log.info("ENABLING nutritionist web security filter chain.");

		http.securityMatcher(new NegatedRequestMatcher(new AntPathRequestMatcher("/rest/mobile/**")))
			.cors(withDefaults())
			.csrf(csrf -> csrf.disable())
			.headers(headers -> headers.frameOptions(options -> options.sameOrigin()))
			.authorizeHttpRequests(ar -> ar.requestMatchers("/rest/subscription/payment/webhook")
				.permitAll()
				.requestMatchers("/invitation/nutritionist/redeem", "/invitation/nutritionist/dev-checkout")
				.authenticated()
				.requestMatchers("/invitation/**")
				.permitAll()
				.requestMatchers("/rest/**")
				.authenticated()
				.requestMatchers("/admin/**")
				.authenticated()
				.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
				.authenticated()
				.anyRequest()
				.permitAll())
			.oauth2Login(withDefaults())
			.logout(logout -> logout.logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
				.addLogoutHandler(logoutHandler));

		return http.build();
	}

}
