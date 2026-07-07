package com.nutriconsultas;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

import com.nutriconsultas.admin.LogoutHandler;
import com.nutriconsultas.security.NutritionistOAuth2AuthorizationRequestResolver;
import com.nutriconsultas.security.NutritionistOAuth2LoginSuccessHandler;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {

	private final LogoutHandler logoutHandler;

	private final NutritionistOAuth2AuthorizationRequestResolver authorizationRequestResolver;

	private final NutritionistOAuth2LoginSuccessHandler loginSuccessHandler;

	public SecurityConfig(final LogoutHandler logoutHandler,
			final NutritionistOAuth2AuthorizationRequestResolver authorizationRequestResolver,
			final NutritionistOAuth2LoginSuccessHandler loginSuccessHandler) {
		this.logoutHandler = logoutHandler;
		this.authorizationRequestResolver = authorizationRequestResolver;
		this.loginSuccessHandler = loginSuccessHandler;
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
				.requestMatchers(HttpMethod.POST, "/rest/webhooks/apple/sign-in")
				.permitAll()
				.requestMatchers("/rest/public/booking/**")
				.permitAll()
				.requestMatchers(HttpMethod.GET, "/invitation/nutritionist/redeem")
				.permitAll()
				.requestMatchers(HttpMethod.GET, "/invitation/clinic/redeem")
				.permitAll()
				.requestMatchers("/invitation/nutritionist/redeem", "/invitation/nutritionist/dev-checkout")
				.authenticated()
				.requestMatchers("/invitation/clinic/redeem")
				.authenticated()
				.requestMatchers("/invitation/**")
				.permitAll()
				.requestMatchers("/rest/**")
				.authenticated()
				.requestMatchers("/mcp/**")
				.authenticated()
				.requestMatchers("/admin/**")
				.authenticated()
				.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
				.authenticated()
				.anyRequest()
				.permitAll())
			.oauth2Login(oauth2 -> oauth2
				.authorizationEndpoint(
						authorization -> authorization.authorizationRequestResolver(authorizationRequestResolver))
				.successHandler(loginSuccessHandler))
			.logout(logout -> logout.logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
				.addLogoutHandler(logoutHandler));

		return http.build();
	}

}
