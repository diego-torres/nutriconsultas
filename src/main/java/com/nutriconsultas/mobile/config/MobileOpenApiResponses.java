package com.nutriconsultas.mobile.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * Shared OpenAPI response declarations for {@code /rest/mobile/patient/**} (#112).
 */
public interface MobileOpenApiResponses {

	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@ApiResponses({ @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
			@ApiResponse(responseCode = "403", description = "Patient account not linked to Auth0 sub") })
	@interface AuthenticatedPatient {

	}

	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@ApiResponses({ @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
			@ApiResponse(responseCode = "403", description = "Subscription limit or entitlement denied") })
	@interface AuthenticatedNutritionist {

	}

	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@ApiResponses({ @ApiResponse(responseCode = "404", description = "Resource not found or not owned by patient") })
	@interface NotFoundWhenMissing {

	}

	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@ApiResponses({ @ApiResponse(responseCode = "400", description = "Validation failed"),
			@ApiResponse(responseCode = "429", description = "Rate limit exceeded (Retry-After: 60)") })
	@interface WriteEndpoint {

	}

}
