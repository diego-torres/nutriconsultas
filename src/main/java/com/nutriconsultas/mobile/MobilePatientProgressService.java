package com.nutriconsultas.mobile;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.clinical.exam.AnthropometricMeasurement;
import com.nutriconsultas.clinical.exam.AnthropometricMeasurementRepository;
import com.nutriconsultas.mobile.dto.PatientProgressSnapshotDto;
import com.nutriconsultas.mobile.dto.ProgressCircumferenceDto;
import com.nutriconsultas.mobile.dto.ProgressMeasurementPointDto;
import com.nutriconsultas.mobile.dto.ProgressMeasurementsDto;
import com.nutriconsultas.paciente.NivelPeso;
import com.nutriconsultas.paciente.NivelPesoLabels;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteAvatarCatalog;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.metrics.BodyMetricRecord;
import com.nutriconsultas.paciente.metrics.BodyMetricRecordService;
import com.nutriconsultas.paciente.metrics.BodyMetricSource;
import com.nutriconsultas.util.ImcGaugeUtils;
import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MobilePatientProgressService {

	private static final int DEFAULT_MAX_ROWS = 365;

	private static final int MAX_ROWS_CAP = 365;

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
				deltaImc, loadCircumferences(pacienteId), PacienteAvatarCatalog.resolveSelectedId(paciente),
				PacienteAvatarCatalog.resolveImagePath(paciente));
	}

	@Transactional(readOnly = true)
	public ProgressMeasurementsDto listMeasurements(final Long pacienteId, final Instant from, final Instant to,
			final Integer maxRows) {
		bodyMetricRecordService.ensureBackfilled(pacienteId);
		final Date fromDate = from != null ? Date.from(from) : null;
		final Date toDate = to != null ? Date.from(to) : null;
		if (fromDate != null && toDate != null && fromDate.after(toDate)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		}
		final int safeMaxRows = resolveMaxRows(maxRows);
		final long totalMatching = bodyMetricRecordRepository.countPatientTimeline(pacienteId, fromDate, toDate);
		final List<BodyMetricRecord> records = bodyMetricRecordRepository.findPatientTimeline(pacienteId, fromDate,
				toDate, PageRequest.of(0, safeMaxRows));
		final Map<Long, ProgressCircumferenceDto> circumferencesBySourceId = loadCircumferencesForRecords(pacienteId,
				records);
		final List<ProgressMeasurementPointDto> points = records.stream()
			.map(record -> toMeasurementPoint(record, circumferencesBySourceId))
			.toList();
		if (log.isDebugEnabled()) {
			log.debug("Listed {} mobile progress measurements (truncated={}) for patient {}", points.size(),
					totalMatching > safeMaxRows, LogRedaction.redactPaciente(pacienteId));
		}
		return new ProgressMeasurementsDto(points, points.size(), totalMatching > safeMaxRows);
	}

	private static int resolveMaxRows(final Integer maxRows) {
		if (maxRows == null) {
			return DEFAULT_MAX_ROWS;
		}
		return Math.min(Math.max(maxRows, 1), MAX_ROWS_CAP);
	}

	private Map<Long, ProgressCircumferenceDto> loadCircumferencesForRecords(final Long pacienteId,
			final List<BodyMetricRecord> records) {
		final List<Long> anthropometricSourceIds = records.stream()
			.filter(record -> record.getSource() == BodyMetricSource.ANTHROPOMETRIC)
			.map(BodyMetricRecord::getSourceId)
			.toList();
		if (anthropometricSourceIds.isEmpty()) {
			return Map.of();
		}
		return anthropometricMeasurementRepository.findByPacienteIdAndIdIn(pacienteId, anthropometricSourceIds)
			.stream()
			.collect(Collectors.toMap(AnthropometricMeasurement::getId, this::toCircumferenceDto,
					(existing, replacement) -> existing));
	}

	private ProgressMeasurementPointDto toMeasurementPoint(final BodyMetricRecord record,
			final Map<Long, ProgressCircumferenceDto> circumferencesBySourceId) {
		final ProgressCircumferenceDto circumferences = record.getSource() == BodyMetricSource.ANTHROPOMETRIC
				? circumferencesBySourceId.get(record.getSourceId()) : null;
		return new ProgressMeasurementPointDto(toInstant(record.getRecordedAt()), record.getWeight(),
				record.getHeight(), record.getImc(), resolveBodyFatPercentage(record), circumferences);
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
