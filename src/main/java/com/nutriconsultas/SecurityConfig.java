package com.nutriconsultas;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.nutriconsultas.admin.LogoutHandler;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {

  private final LogoutHandler logoutHandler;

  public SecurityConfig(LogoutHandler logoutHandler) {
    this.logoutHandler = logoutHandler;
  }

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    log.info("ENABLING security filter chain.");

      http.cors(withDefaults())
              .csrf(csrf -> csrf.disable())
              .headers(headers -> headers
                      .frameOptions(options -> options.sameOrigin()))
              .authorizeHttpRequests(
                      ar -> ar.requestMatchers("/rest/**").authenticated()
                              .requestMatchers("/admin/**").authenticated()
                              .anyRequest().permitAll())
              .oauth2Login(withDefaults())
              .logout(logout -> logout.logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                      .addLogoutHandler(logoutHandler));

    return http.build();
  }
}
