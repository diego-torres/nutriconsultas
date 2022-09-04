package com.nutriconsultas;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

  @Override
  protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    PasswordEncoder encoder = 
        PasswordEncoderFactories.createDelegatingPasswordEncoder();
    auth
        .inMemoryAuthentication()
        .withUser("user")
        .password(encoder.encode("password"))
        .roles("USER")
        .and()
        .withUser("admin")
        .password(encoder.encode("admin"))
        .roles("USER", "ADMIN");
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    // super.configure(http);
    http.csrf().disable()
    .cors()
    .and()
    .authorizeRequests()
    .antMatchers("/admin/**")
    .hasRole("admin")
    .antMatchers("/personal/**")
    .hasRole("user")
    .anyRequest()
    .permitAll();
  }
}
