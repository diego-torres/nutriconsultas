package com.nutriconsultas.mobile.dto;

import java.time.Instant;

public record ProgressMeasurementPointDto(Instant recordedAt, Double weightKg, Double heightM, Double bmi,
		Double bodyFatPercentage, ProgressCircumferenceDto circumferences) {
}
