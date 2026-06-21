package com.nutriconsultas.mobile.dto;

import java.time.Instant;

import com.nutriconsultas.paciente.NivelPeso;

public record PatientProgressSnapshotDto(Instant latestMeasurementAt, Instant previousMeasurementAt, Double weightKg,
		Double heightM, Double bmi, NivelPeso nivelPeso, String imcLabel, Double bmr, Double bodyFatPercentage,
		Double deltaPeso, Double deltaImc, ProgressCircumferenceDto circumferences, String avatarId, String avatarUrl) {
}
