package com.nutriconsultas.paciente.metrics;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.calendar.CalendarEventRepository;
import com.nutriconsultas.charts.ChartResponse;
import com.nutriconsultas.clinical.exam.AnthropometricMeasurement;
import com.nutriconsultas.clinical.exam.AnthropometricMeasurementRepository;
import com.nutriconsultas.clinical.exam.ClinicalExam;
import com.nutriconsultas.clinical.exam.ClinicalExamRepository;
import com.nutriconsultas.paciente.NivelPeso;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.calculation.BmrCalculationService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BodyMetricRecordServiceImpl implements BodyMetricRecordService {

	private final BodyMetricRecordRepository repository;

	private final CalendarEventRepository calendarEventRepository;

	private final AnthropometricMeasurementRepository anthropometricMeasurementRepository;

	private final ClinicalExamRepository clinicalExamRepository;

	private final PacienteRepository pacienteRepository;

	public BodyMetricRecordServiceImpl(final BodyMetricRecordRepository repository,
			final CalendarEventRepository calendarEventRepository,
			final AnthropometricMeasurementRepository anthropometricMeasurementRepository,
			final ClinicalExamRepository clinicalExamRepository, final PacienteRepository pacienteRepository) {
		this.repository = repository;
		this.calendarEventRepository = calendarEventRepository;
		this.anthropometricMeasurementRepository = anthropometricMeasurementRepository;
		this.clinicalExamRepository = clinicalExamRepository;
		this.pacienteRepository = pacienteRepository;
	}

	@Override
	@Transactional
	public void syncFromConsultation(@NonNull final CalendarEvent event) {
		if (event.getId() == null || event.getPaciente() == null || event.getEventDateTime() == null) {
			return;
		}
		upsertRecord(event.getPaciente(), event.getEventDateTime(), BodyMetricSource.CONSULTATION, event.getId(),
				event.getPeso(), event.getEstatura(), event.getImc(), event.getNivelPeso(),
				event.getIndiceGrasaCorporal(), null);
	}

	@Override
	@Transactional
	public void syncFromAnthropometric(@NonNull final AnthropometricMeasurement measurement) {
		if (measurement.getId() == null || measurement.getPaciente() == null
				|| measurement.getMeasurementDateTime() == null) {
			return;
		}
		upsertRecord(measurement.getPaciente(), measurement.getMeasurementDateTime(), BodyMetricSource.ANTHROPOMETRIC,
				measurement.getId(), measurement.getPeso(), measurement.getEstatura(), measurement.getImc(),
				measurement.getNivelPeso(), measurement.getIndiceGrasaCorporal(),
				measurement.getPorcentajeGrasaCorporal());
	}

	@Override
	@Transactional
	public void syncFromClinicalExam(@NonNull final ClinicalExam exam) {
		if (exam.getId() == null || exam.getPaciente() == null || exam.getExamDateTime() == null) {
			return;
		}
		upsertRecord(exam.getPaciente(), exam.getExamDateTime(), BodyMetricSource.CLINICAL_EXAM, exam.getId(),
				exam.getPeso(), exam.getEstatura(), exam.getImc(), exam.getNivelPeso(), exam.getIndiceGrasaCorporal(),
				null);
	}

	@Override
	@Transactional
	public void ensureBackfilled(@NonNull final Long pacienteId) {
		if (repository.existsByPacienteId(pacienteId)) {
			return;
		}
		log.info("Backfilling body metric history for paciente id {}", pacienteId);
		for (final CalendarEvent event : calendarEventRepository.findByPacienteId(pacienteId)) {
			syncFromConsultation(event);
		}
		for (final AnthropometricMeasurement measurement : anthropometricMeasurementRepository
			.findByPacienteId(pacienteId)) {
			syncFromAnthropometric(measurement);
		}
		for (final ClinicalExam exam : clinicalExamRepository.findByPacienteId(pacienteId)) {
			syncFromClinicalExam(exam);
		}
	}

	@Override
	@Transactional
	public void removeSourceAndRefreshPatient(@NonNull final BodyMetricSource source, @NonNull final Long sourceId,
			@NonNull final Long pacienteId) {
		repository.deleteBySourceAndSourceId(source, sourceId);
		refreshPatientSnapshot(pacienteId);
	}

	@Override
	@Transactional
	public Optional<BodyMetricRecord> findLatestByPacienteId(@NonNull final Long pacienteId) {
		ensureBackfilled(pacienteId);
		return repository.findFirstByPacienteIdOrderByRecordedAtDescIdDesc(pacienteId);
	}

	@Override
	@Transactional
	public ChartResponse buildChartResponse(@NonNull final Long pacienteId) {
		ensureBackfilled(pacienteId);
		final List<BodyMetricRecord> records = repository.findByPacienteIdOrderByRecordedAtAsc(pacienteId);
		final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

		final List<BodyMetricRecord> imcRecords = records.stream()
			.filter(record -> record.getImc() != null)
			.collect(Collectors.toList());
		final List<String> imcLabels = imcRecords.stream()
			.map(record -> dateFormat.format(record.getRecordedAt()))
			.collect(Collectors.toList());
		final List<Double> imcData = imcRecords.stream().map(BodyMetricRecord::getImc).collect(Collectors.toList());

		final List<BodyMetricRecord> fatRecords = records.stream()
			.filter(record -> resolveBodyFatValue(record) != null)
			.collect(Collectors.toList());
		final List<String> fatLabels = fatRecords.stream()
			.map(record -> dateFormat.format(record.getRecordedAt()))
			.collect(Collectors.toList());
		final List<Double> fatData = fatRecords.stream().map(this::resolveBodyFatValue).collect(Collectors.toList());

		final Map<String, Object> data = new HashMap<>();
		data.put("imc", imcData);
		data.put("grasaCorporal", fatData);
		data.put("imcLabels", imcLabels);
		data.put("grasaCorporalLabels", fatLabels);

		// Legacy keys: use IMC timeline as default labels for backward-compatible clients
		final List<String> labels = imcLabels.isEmpty() ? fatLabels : imcLabels;
		data.put("peso", imcRecords.stream().map(BodyMetricRecord::getWeight).collect(Collectors.toList()));
		data.put("estatura", imcRecords.stream().map(BodyMetricRecord::getHeight).collect(Collectors.toList()));

		return new ChartResponse(labels, data);
	}

	private void upsertRecord(final Paciente paciente, final java.util.Date recordedAt, final BodyMetricSource source,
			final Long sourceId, final Double weight, final Double height, final Double imc, final NivelPeso nivelPeso,
			final Double bodyFatIndex, final Double bodyFatPercentage) {
		if (!hasBodyMetricData(weight, height, imc, bodyFatIndex, bodyFatPercentage)) {
			repository.deleteBySourceAndSourceId(source, sourceId);
			return;
		}

		final BodyMetricRecord record = repository.findBySourceAndSourceId(source, sourceId)
			.orElseGet(BodyMetricRecord::new);
		record.setPaciente(paciente);
		record.setRecordedAt(recordedAt);
		record.setSource(source);
		record.setSourceId(sourceId);
		record.setWeight(weight);
		record.setHeight(height);
		record.setImc(imc);
		record.setNivelPeso(nivelPeso);
		record.setBodyFatIndex(bodyFatIndex);
		record.setBodyFatPercentage(bodyFatPercentage);
		repository.save(record);
	}

	private boolean hasBodyMetricData(final Double weight, final Double height, final Double imc,
			final Double bodyFatIndex, final Double bodyFatPercentage) {
		return weight != null || height != null || imc != null || bodyFatIndex != null || bodyFatPercentage != null;
	}

	private Double resolveBodyFatValue(final BodyMetricRecord record) {
		if (record.getBodyFatIndex() != null) {
			return record.getBodyFatIndex();
		}
		return record.getBodyFatPercentage();
	}

	private void refreshPatientSnapshot(@NonNull final Long pacienteId) {
		final Paciente paciente = pacienteRepository.findById(pacienteId).orElse(null);
		if (paciente == null) {
			return;
		}
		final Optional<BodyMetricRecord> latest = repository
			.findFirstByPacienteIdOrderByRecordedAtDescIdDesc(pacienteId);
		if (latest.isPresent()) {
			final BodyMetricRecord record = latest.get();
			paciente.setPeso(record.getWeight());
			paciente.setEstatura(record.getHeight());
			paciente.setImc(record.getImc());
			paciente.setNivelPeso(record.getNivelPeso());
		}
		else {
			paciente.setPeso(null);
			paciente.setEstatura(null);
			paciente.setImc(null);
			paciente.setNivelPeso(null);
			paciente.setBmr(null);
		}
		refreshPatientBmrIfPossible(paciente);
		pacienteRepository.save(paciente);
		log.debug("Refreshed patient {} body metrics after history update", pacienteId);
	}

	private void refreshPatientBmrIfPossible(final Paciente paciente) {
		if (paciente.getPeso() == null || paciente.getEstatura() == null) {
			return;
		}
		final Integer age = calculateAge(paciente.getDob());
		final Boolean isMale = "M".equalsIgnoreCase(paciente.getGender());
		final Double bmr = BmrCalculationService.calculatePromedioBmr(paciente.getPeso(), paciente.getEstatura(), age,
				isMale);
		if (bmr != null) {
			paciente.setBmr(bmr);
		}
	}

	private Integer calculateAge(final java.util.Date dob) {
		if (dob == null) {
			return null;
		}
		final java.util.Date utilDate = dob instanceof java.sql.Date ? new java.util.Date(dob.getTime())
				: (java.util.Date) dob;
		final LocalDate birthDate = utilDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		final LocalDate currentDate = LocalDate.now();
		if (birthDate.isAfter(currentDate)) {
			return null;
		}
		return Period.between(birthDate, currentDate).getYears();
	}

}
