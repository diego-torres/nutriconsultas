package com.nutriconsultas.mobile.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nutriconsultas.paciente.PacienteDietaStatus;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Lightweight assigned diet plan pointer for onboarding bootstrap (#138).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Assigned diet plan reference")
public record AssignedDietPlanReferenceDto(
		@Schema(description = "PacienteDieta assignment id", example = "55") Long assignmentId,
		@Schema(description = "Assignment status", example = "ACTIVE") PacienteDietaStatus status,
		@Schema(description = "Diet display name", example = "Plan hipocalórico") String dietaName) {
}
