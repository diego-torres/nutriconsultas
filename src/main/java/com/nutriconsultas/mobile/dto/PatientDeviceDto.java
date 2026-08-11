package com.nutriconsultas.mobile.dto;

import java.time.Instant;

import com.nutriconsultas.device.PatientDevice;
import com.nutriconsultas.device.PatientDevicePlatform;

public record PatientDeviceDto(Long id, PatientDevicePlatform platform, Instant updatedAt) {

	public static PatientDeviceDto fromEntity(final PatientDevice device) {
		return new PatientDeviceDto(device.getId(), device.getPlatform(), device.getUpdatedAt());
	}

}
