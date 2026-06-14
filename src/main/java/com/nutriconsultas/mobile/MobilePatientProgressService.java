package com.nutriconsultas.mobile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.clinical.exam.AnthropometricMeasurement;
import com.nutriconsultas.clinical.exam.AnthropometricMeasurementRepository;
import com.nutriconsultas.mobile.dto.PatientProgressSnapshotDto;
import com.nutriconsultas.mobile.dto.ProgressCircumferenceDto;
import com.nutriconsultas.paciente.NivelPeso;
import com.nutriconsultas.paciente.NivelPesoLabels;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.metrics.BodyMetricRecord;
import com.nutriconsultas.paciente.metrics.BodyMetricRecordService;
import com.nutriconsultas.util.ImcGaugeUtils;
import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MobilePatientProgressService {

	private final BodyMetricRecordService bodyMetricRecordService;

	private final com.nutriconsultas.paciente.metrics.BodyMetricRecordRepository bodyMetricRecordRepository;

	private final AnthropometricMeasurementRepository anthropometricMeasurementRepository;

	private final PacienteRepository pacienteRepository;

	public MobilePatientProgressService(final BodyMetricRecordService bodyMetricRecordService,
			final com.nutriconsultas.paciente.metrics.BodyMetricRecordRepository bodyMetricRecordRepository,
			final AnthropometricMeasurementRepository anthropometricMeasurementRepository,
			final PacienteRepository pacienteRepository) {
		this.bodyMetricRecordService = bodyMetricRecordService;
		this.bodyMetricRecordRepository = bodyMetricRecordRepository;
		this.anthropometricMeasurementRepository = anthropometricMeasurementRepository;
		this.pacienteRepository = pacienteRepository;
	}

	@Transactional(readOnly = true)
	public PatientProgressSnapshotDto getSnapshot(final Long pacienteId) {
		final Paciente paciente = pacienteRepository.findById(pacienteId)
			.orElseThrow(() -> new IllegalArgumentException("Patient not found"));
		bodyMetricRecordService.ensureBackfilled(pacienteId);
		final List<BodyMetricRecord> recentRecords = bodyMetricRecordRepository
			.findTop2ByPacienteIdOrderByRecordedAtDescIdDesc(pacienteId);
		final Optional<BodyMetricRecord> latestRecord = recentRecords.stream().findFirst();
		final Optional<BodyMetricRecord> previousRecord = recentRecords.size() > 1 ? Optional.of(recentRecords.get(1))
				: Optional.empty();

		final Double weightKg = firstNonNull(latestRecord.map(BodyMetricRecord::getWeight).orElse(null),
				paciente.getPeso());
		final Double heightM = firstNonNull(latestRecord.map(BodyMetricRecord::getHeight).orElse(null),
				paciente.getEstatura());
		final Double bmi = firstNonNull(latestRecord.map(BodyMetricRecord::getImc).orElse(null), paciente.getImc());
		final NivelPeso nivelPeso = ImcGaugeUtils.resolveNivelPeso(bmi,
				firstNonNull(latestRecord.map(BodyMetricRecord::getNivelPeso).orElse(null), paciente.getNivelPeso()));
		final Double bodyFatPercentage = latestRecord.map(this::resolveBodyFatPercentage).orElse(null);
		final Double deltaPeso = computeDelta(latestRecord.map(BodyMetricRecord::getWeight).orElse(null),
				previousRecord.map(BodyMetricRecord::getWeight).orElse(null));
		final Double deltaImc = computeDelta(latestRecord.map(BodyMetricRecord::getImc).orElse(null),
				previousRecord.map(BodyMetricRecord::getImc).orElse(null));

		if (log.isDebugEnabled()) {
			log.debug("Built mobile progress snapshot for patient {}", LogRedaction.redactPaciente(pacienteId));
		}

		return new PatientProgressSnapshotDto(toInstant(latestRecord.map(BodyMetricRecord::getRecordedAt).orElse(null)),
				toInstant(previousRecord.map(BodyMetricRecord::getRecordedAt).orElse(null)), weightKg, heightM, bmi,
				nivelPeso, NivelPesoLabels.toImcLabel(nivelPeso), paciente.getBmr(), bodyFatPercentage, deltaPeso,
				deltaImc, loadCircumferences(pacienteId));
	}

	private ProgressCircumferenceDto loadCircumferences(final Long pacienteId) {
		return anthropometricMeasurementRepository.findFirstByPacienteIdOrderByMeasurementDateTimeDescIdDesc(pacienteId)
			.map(this::toCircumferenceDto)
			.orElse(null);
	}

	private ProgressCircumferenceDto toCircumferenceDto(final AnthropometricMeasurement measurement) {
		final Double waist = measurement.getCintura();
		final Double hip = measurement.getCadera();
		if (waist == null && hip == null) {
			return null;
		}
		return new ProgressCircumferenceDto(waist, hip);
	}

	private Double resolveBodyFatPercentage(final BodyMetricRecord record) {
		if (record.getBodyFatPercentage() != null) {
			return record.getBodyFatPercentage();
		}
		return record.getBodyFatIndex();
	}

	private static Double computeDelta(final Double latest, final Double previous) {
		if (latest == null || previous == null) {
			return null;
		}
		return latest - previous;
	}

	private static Instant toInstant(final java.util.Date date) {
		if (date == null) {
			return null;
		}
		return date.toInstant();
	}

	@SafeVarargs
	private static <T> T firstNonNull(final T... values) {
		for (final T value : values) {
			if (value != null) {
				return value;
			}
		}
		return null;
	}

}
