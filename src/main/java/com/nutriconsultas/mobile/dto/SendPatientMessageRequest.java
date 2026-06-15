package com.nutriconsultas.mobile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendPatientMessageRequest(@NotBlank @Size(max = 2000) @Schema(minLength = 1, maxLength = 2000,
		description = "Message body") String body) {
}
