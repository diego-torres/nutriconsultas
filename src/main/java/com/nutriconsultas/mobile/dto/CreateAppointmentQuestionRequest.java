package com.nutriconsultas.mobile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAppointmentQuestionRequest(@NotBlank @Size(max = 2000) @Schema(minLength = 1, maxLength = 2000,
		description = "Question text to remember for the next appointment") String body) {
}
