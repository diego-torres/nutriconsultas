package com.nutriconsultas.mobile.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Nutritionist request to create a patient and issue an onboarding invitation (#134).
 */
@Schema(description = "Create patient + invitation")
public record CreatePatientInvitationRequest(
		@NotBlank @Size(max = 100) @Schema(description = "Patient legal/clinical name",
				example = "María López") String name,
		@NotBlank @Email @Size(max = 100) @Schema(description = "Delivery email for the invite",
				example = "maria@example.com") String email,
		@NotNull @Schema(description = "Date of birth (ISO-8601 date)", example = "1990-05-15") LocalDate dob,
		@NotBlank @Pattern(regexp = "(?i)^[MF]$", message = "must be M or F") @Schema(description = "Gender (M/F)",
				example = "F") String gender,
		@Size(max = 25) @Schema(description = "Optional phone", example = "+525512345678") String phone,
		@Size(max = 50) @Schema(description = "Optional clinic-facing identifier; auto-generated when omitted",
				example = "P-CLINIC-001") String assignedId,
		@Size(max = 100) @Schema(description = "Optional display name for onboarding UI",
				example = "María") String displayName) {
}
