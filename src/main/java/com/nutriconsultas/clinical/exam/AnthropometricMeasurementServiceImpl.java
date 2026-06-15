package com.nutriconsultas.clinical.exam;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.calculation.BmrFormulaType;
import com.nutriconsultas.paciente.calculation.EnergyExpenditureResolver;
import com.nutriconsultas.paciente.calculation.PatientEnergyPreferences;
import com.nutriconsultas.paciente.calculation.PhysicalActivityLevel;
import com.nutriconsultas.util.LogRedaction;

import com.nutriconsultas.paciente.metrics.BodyMetricSource;

import java.time.Instant;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AnthropometricMeasurementServiceImpl implements AnthropometricMeasurementService {

	@Autowired
	private AnthropometricMeasurementRepository repository;

	@Autowired
	private com.nutriconsultas.paciente.metrics.BodyMetricRecordService bodyMetricRecordService;

	@Autowired
	private PacienteRepository pacienteRepository;

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
		applyEnergyCalculation(measurement);
		final AnthropometricMeasurement saved = repository.save(measurement);
		bodyMetricRecordService.syncFromAnthropometric(saved);
		syncPatientEnergySnapshot(saved);
		return saved;
	}

	private void applyEnergyCalculation(final AnthropometricMeasurement measurement) {
		if (measurement.getPhysicalActivityLevel() == null || measurement.getPaciente() == null) {
			return;
		}
		final Double weight = measurement.getPeso();
		final Double height = measurement.getEstatura();
		if (weight == null || height == null) {
			return;
		}
		final BmrFormulaType formula = measurement.getBmrFormula() != null ? measurement.getBmrFormula()
				: PatientEnergyPreferences.resolveBmrFormula(measurement.getPaciente());
		final Double customFactor = measurement.getPhysicalActivityLevel() == PhysicalActivityLevel.CUSTOM
				? measurement.getActivityFactor() : null;
		final EnergyExpenditureResolver.EnergyResult result = EnergyExpenditureResolver.resolve(
				measurement.getPaciente(), formula, measurement.getPhysicalActivityLevel(), customFactor, weight,
				height, null, measurement, null);
		measurement.setBmrFormula(formula);
		measurement.setBmrUsed(result.bmr());
		measurement.setActivityFactor(result.activityFactor());
		measurement.setGetKcal(result.getKcal());
		measurement.setTefKcal(result.tefKcal());
		measurement.setTotalAdjustedKcal(result.totalAdjustedKcal());
		measurement.setStressKcal(result.stressKcal());
		measurement.setFinalTotalKcal(result.finalTotalKcal());
	}

	private void syncPatientEnergySnapshot(final AnthropometricMeasurement measurement) {
		if (measurement.getGetKcal() == null || measurement.getPaciente() == null) {
			return;
		}
		final Paciente paciente = measurement.getPaciente();
		EnergyExpenditureResolver.applyToPatient(paciente,
				new EnergyExpenditureResolver.EnergyResult(measurement.getBmrUsed(), measurement.getActivityFactor(),
						measurement.getGetKcal(), null, measurement.getTefKcal(), measurement.getTotalAdjustedKcal(),
						measurement.getStressKcal(), measurement.getFinalTotalKcal()),
				measurement.getPhysicalActivityLevel());
		pacienteRepository.save(paciente);
	}

	@Override
	@Transactional
	public void deleteById(@NonNull final Long id) {
		log.debug("Deleting anthropometric measurement with id: {}", id);
		final AnthropometricMeasurement measurement = repository.findById(id).orElse(null);
		if (measurement != null) {
			final Long pacienteId = measurement.getPaciente() != null ? measurement.getPaciente().getId() : null;
			repository.deleteById(id);
			if (pacienteId != null) {
				bodyMetricRecordService.removeSourceAndRefreshPatient(BodyMetricSource.ANTHROPOMETRIC, id, pacienteId);
			}
		}
		else {
			repository.deleteById(id);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<AnthropometricMeasurement> findByPacienteId(@NonNull final Long pacienteId) {
		log.debug("Finding anthropometric measurements for paciente id: {}", pacienteId);
		return repository.findByPacienteId(pacienteId);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<LatestBodyFatResult> findLatestBodyFatForPatient(@NonNull final Long pacienteId,
			@NonNull final String userId) {
		log.debug("Finding latest body fat for paciente id {}", pacienteId);
		final Paciente paciente = pacienteRepository.findByIdAndUserId(pacienteId, userId).orElse(null);
		if (paciente == null) {
			return Optional.empty();
		}
		return repository.findFirstByPacienteIdOrderByMeasurementDateTimeDescIdDesc(pacienteId)
			.flatMap(this::resolveLatestBodyFat);
	}

	private Optional<LatestBodyFatResult> resolveLatestBodyFat(final AnthropometricMeasurement measurement) {
		final Instant measurementDate = measurement.getMeasurementDateTime() != null
				? measurement.getMeasurementDateTime().toInstant() : null;
		if (measurement.getPorcentajeGrasaCorporal() != null) {
			return Optional.of(new LatestBodyFatResult(measurement.getPorcentajeGrasaCorporal(), measurementDate,
					BodyFatSource.PORCENTAJE));
		}
		if (measurement.getBioimpedance() != null && measurement.getBioimpedance().getBodyFatPercentage() != null) {
			return Optional.of(new LatestBodyFatResult(measurement.getBioimpedance().getBodyFatPercentage(),
					measurementDate, BodyFatSource.BIOIMPEDANCE));
		}
		if (measurement.getIndiceGrasaCorporal() != null) {
			return Optional.of(new LatestBodyFatResult(measurement.getIndiceGrasaCorporal(), measurementDate,
					BodyFatSource.INDICE));
		}
		return Optional.empty();
	}

}
