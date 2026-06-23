package com.nutriconsultas.mobile.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Partial onboarding profile update for {@code PATCH /rest/mobile/patient/me} (#138).
 */
@Schema(description = "Patch patient onboarding profile")
public record PatchPatientOnboardingProfileRequest(
		@Size(max = 100) @Schema(description = "Legal/clinical name", example = "María López") String name,
		@Size(max = 100) @Schema(description = "Display name for mobile UI", example = "María") String displayName,
		@Schema(description = "Date of birth (ISO-8601 date)", example = "1990-05-15") LocalDate dob,
		@Pattern(regexp = "(?i)^[MF]$", message = "must be M or F") @Schema(description = "Gender (M/F)",
				example = "F") String gender,
		@Email @Size(max = 100) @Schema(description = "Contact email", example = "maria@example.com") String email,
		@Size(max = 25) @Schema(description = "Contact phone", example = "+525512345678") String phone,
		@Size(max = 32) @Schema(description = "Avatar id from catalog", example = "avatar_6") String avatarId) {
}
