package com.nutriconsultas.mobile.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PatientSignupRequest(@NotBlank @Email @Schema(description = "Patient email") String email,
		@NotBlank @Size(min = 8, max = 128) @Schema(description = "Account password") String password,
		@Size(max = 120) @Schema(description = "Display name stored in Auth0 user metadata") String displayName,
		@Schema(description = "Raw invite URL token") String token,
		@Schema(description = "Human-readable invitation code") String humanCode) {

}
