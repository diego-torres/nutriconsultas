package com.nutriconsultas.mobile.dto;

import com.nutriconsultas.device.PatientDevicePlatform;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterPatientDeviceRequest(@NotNull @Schema(description = "Device push platform", allowableValues = {
		"IOS", "ANDROID" }) PatientDevicePlatform platform,
		@NotBlank @Size(max = 512) @Schema(minLength = 1, maxLength = 512,
				description = "APNs or FCM registration token") String token,
		@Size(max = 50) @Schema(maxLength = 50, description = "Optional mobile app version") String appVersion) {
}
