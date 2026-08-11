package com.nutriconsultas.mobile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeregisterPatientDeviceRequest(@NotBlank @Size(max = 512) @Schema(minLength = 1, maxLength = 512,
		description = "APNs or FCM registration token to remove") String token) {
}
