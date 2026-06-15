package com.nutriconsultas.mobile.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springdoc.core.models.GroupedOpenApi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class MobileOpenApiConfig {

	private static final String BEARER_JWT_SCHEME = "bearer-jwt";

	@Bean
	public GroupedOpenApi mobileApi() {
		return GroupedOpenApi.builder().group("mobile").pathsToMatch("/rest/mobile/**").build();
	}

	@Bean
	public OpenAPI mobileOpenApi(@Value("${app.security.jwt.audience:}") final String audience) {
		final String audienceNote = audience.isBlank() ? "configured AUTH_AUDIENCE" : audience;
		return new OpenAPI()
			.info(new Info().title("Minutriporcion Patient Mobile API")
				.version("1.0")
				.description("Patient-facing JSON API under /rest/mobile/patient/**. "
						+ "Requires Auth0 Bearer JWT (resource server).")
				.contact(new Contact().name("Minutriporcion").url("https://minutriporcion.com")))
			.components(new Components().addSecuritySchemes(BEARER_JWT_SCHEME,
					new SecurityScheme().type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.bearerFormat("JWT")
						.description("Auth0 access token with audience " + audienceNote)))
			.addSecurityItem(new SecurityRequirement().addList(BEARER_JWT_SCHEME));
	}

}
