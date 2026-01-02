package com.nutriconsultas.clinical.exam;

import java.util.List;

import org.springframework.lang.NonNull;

public interface AnthropometricMeasurementService {

	List<AnthropometricMeasurement> findAll();

	AnthropometricMeasurement findById(@NonNull Long id);

	AnthropometricMeasurement save(@NonNull AnthropometricMeasurement measurement);

	void deleteById(@NonNull Long id);

	List<AnthropometricMeasurement> findByPacienteId(@NonNull Long pacienteId);

}
