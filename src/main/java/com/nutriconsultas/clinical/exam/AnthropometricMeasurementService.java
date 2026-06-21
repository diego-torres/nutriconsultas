package com.nutriconsultas.clinical.exam;

import java.util.List;
import java.util.Optional;

import org.springframework.lang.NonNull;

import com.nutriconsultas.clinical.exam.anthropometric.AnthropometricDerivedFieldsDto;
import com.nutriconsultas.clinical.exam.anthropometric.AnthropometricFieldUpdateRequest;

public interface AnthropometricMeasurementService {

	List<AnthropometricMeasurement> findAll();

	AnthropometricMeasurement findById(@NonNull Long id);

	AnthropometricMeasurement save(@NonNull AnthropometricMeasurement measurement);

	void deleteById(@NonNull Long id);

	List<AnthropometricMeasurement> findByPacienteId(@NonNull Long pacienteId);

	Optional<LatestBodyFatResult> findLatestBodyFatForPatient(@NonNull Long pacienteId, @NonNull String userId);

	AnthropometricDerivedFieldsDto updateCorrectableField(@NonNull Long pacienteId, @NonNull Long measurementId,
			@NonNull String userId, @NonNull AnthropometricFieldUpdateRequest request);

}
