package com.nutriconsultas.clinical.exam;

import java.time.Instant;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.clinical.exam.anthropometric.AnthropometricDerivedFieldsDto;
import com.nutriconsultas.clinical.exam.anthropometric.AnthropometricFieldAccessor;
import com.nutriconsultas.clinical.exam.anthropometric.AnthropometricFieldCatalog;
import com.nutriconsultas.clinical.exam.anthropometric.AnthropometricFieldDefinition;
import com.nutriconsultas.clinical.exam.anthropometric.AnthropometricFieldUpdateRequest;
import com.nutriconsultas.clinical.exam.anthropometric.AnthropometricRecalcGroup;
import com.nutriconsultas.clinical.exam.anthropometric.AnthropometricRecalculationService;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.calculation.BmrFormulaType;
import com.nutriconsultas.paciente.calculation.EnergyExpenditureResolver;
import com.nutriconsultas.paciente.calculation.PatientEnergyPreferences;
import com.nutriconsultas.paciente.calculation.PhysicalActivityLevel;
import com.nutriconsultas.paciente.metrics.BodyMetricSource;
import com.nutriconsultas.util.LogRedaction;

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

	@Autowired
	private AnthropometricRecalculationService recalculationService;

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

	@Override
	@Transactional
	public AnthropometricDerivedFieldsDto updateCorrectableField(@NonNull final Long pacienteId,
			@NonNull final Long measurementId, @NonNull final String userId,
			@NonNull final AnthropometricFieldUpdateRequest request) {
		pacienteRepository.findByIdAndUserId(pacienteId, userId)
			.orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado"));
		final AnthropometricMeasurement measurement = repository.findById(measurementId)
			.orElseThrow(() -> new IllegalArgumentException("Medición antropométrica no encontrada"));
		if (!measurement.getPaciente().getId().equals(pacienteId)) {
			throw new IllegalArgumentException("La medición no pertenece al paciente");
		}
		if (!userId.equals(measurement.getPaciente().getUserId())) {
			throw new IllegalArgumentException("No autorizado");
		}

		final AnthropometricFieldDefinition definition = AnthropometricFieldCatalog.findByKey(request.getFieldKey())
			.orElseThrow(() -> new IllegalArgumentException("Campo no editable: " + request.getFieldKey()));
		validateFieldValue(definition, request.getValue());
		if (definition.confirmDerivedRecalc() && !request.isConfirmDerivedRecalc()) {
			throw new IllegalStateException("Se requiere confirmación para recalcular métricas derivadas");
		}

		AnthropometricFieldAccessor.write(measurement, definition.fieldKey(), request.getValue());
		appendCorrectionNote(measurement, definition, request.getCorrectionNote());
		measurement.setUpdatedAt(new Date());

		final Set<AnthropometricRecalcGroup> groups = recalculationService.groupsForField(definition);
		final Set<AnthropometricRecalcGroup> immediateGroups = EnumSet.copyOf(groups);
		immediateGroups.remove(AnthropometricRecalcGroup.ENERGY);
		immediateGroups.remove(AnthropometricRecalcGroup.PATIENT_SNAPSHOT);
		recalculationService.applyRecalcGroups(measurement, immediateGroups, request.getValue());

		if (groups.contains(AnthropometricRecalcGroup.ENERGY)) {
			applyEnergyCalculation(measurement);
		}
		if (groups.contains(AnthropometricRecalcGroup.PATIENT_SNAPSHOT)) {
			recalculationService.applyRecalcGroups(measurement, EnumSet.of(AnthropometricRecalcGroup.PATIENT_SNAPSHOT),
					request.getValue());
		}

		final AnthropometricMeasurement saved = repository.save(measurement);
		bodyMetricRecordService.syncFromAnthropometric(saved);
		if (groups.contains(AnthropometricRecalcGroup.ENERGY)) {
			syncPatientEnergySnapshot(saved);
		}

		log.info("Anthropometric field corrected: measurementId={}, fieldKey={}", measurementId, definition.fieldKey());
		return recalculationService.toDerivedFields(saved, request.getValue());
	}

	private void validateFieldValue(final AnthropometricFieldDefinition definition, final Double value) {
		if (value == null) {
			throw new IllegalArgumentException("El valor es requerido");
		}
		if (definition.minValue() != null && value < definition.minValue()) {
			throw new IllegalArgumentException(
					"Valor fuera de rango (mínimo " + definition.minValue() + " " + definition.unit() + ")");
		}
		if (definition.maxValue() != null && value > definition.maxValue()) {
			throw new IllegalArgumentException(
					"Valor fuera de rango (máximo " + definition.maxValue() + " " + definition.unit() + ")");
		}
	}

	private void appendCorrectionNote(final AnthropometricMeasurement measurement,
			final AnthropometricFieldDefinition definition, final String correctionNote) {
		if (correctionNote == null || correctionNote.isBlank()) {
			return;
		}
		final String sanitized = correctionNote.trim().replaceAll("[\\r\\n]+", " ");
		if (sanitized.length() > 500) {
			throw new IllegalArgumentException("La nota de corrección no puede exceder 500 caracteres");
		}
		final String entry = String.format("[Corrección %s — %s: %s]",
				new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()), definition.label(), sanitized);
		final String existing = measurement.getNotes();
		measurement.setNotes(existing == null || existing.isBlank() ? entry : existing + "\n" + entry);
	}

}
