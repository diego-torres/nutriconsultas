package com.nutriconsultas.mobile.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PatientLoginRequest(@NotBlank @Email @Schema(description = "Patient email") String email,
		@NotBlank @Size(min = 1, max = 128) @Schema(description = "Account password") String password,
		@Schema(description = "Optional invite URL token for first-login gate") String token,
		@Schema(description = "Optional human invitation code for first-login gate") String humanCode) {

}
