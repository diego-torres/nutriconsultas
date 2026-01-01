package com.nutriconsultas.clinical.exam;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnthropometricMeasurementRepository extends JpaRepository<AnthropometricMeasurement, Long> {

	List<AnthropometricMeasurement> findByPacienteId(Long pacienteId);

}

