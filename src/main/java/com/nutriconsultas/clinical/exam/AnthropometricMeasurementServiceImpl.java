package com.nutriconsultas.clinical.exam;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AnthropometricMeasurementServiceImpl implements AnthropometricMeasurementService {

	@Autowired
	private AnthropometricMeasurementRepository repository;

	@Override
	@Transactional(readOnly = true)
	public List<AnthropometricMeasurement> findAll() {
		log.debug("Finding all anthropometric measurements");
		return repository.findAll();
	}

	@Override
	@Transactional(readOnly = true)
	public AnthropometricMeasurement findById(@NonNull final Long id) {
		log.debug("Finding anthropometric measurement with id: {}", id);
		return repository.findById(id).orElse(null);
	}

	@Override
	@Transactional
	public AnthropometricMeasurement save(@NonNull final AnthropometricMeasurement measurement) {
		log.debug("Saving anthropometric measurement: {}", LogRedaction.redactAnthropometricMeasurement(measurement));
		return repository.save(measurement);
	}

	@Override
	@Transactional
	public void deleteById(@NonNull final Long id) {
		log.debug("Deleting anthropometric measurement with id: {}", id);
		repository.deleteById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public List<AnthropometricMeasurement> findByPacienteId(@NonNull final Long pacienteId) {
		log.debug("Finding anthropometric measurements for paciente id: {}", pacienteId);
		return repository.findByPacienteId(pacienteId);
	}

}
