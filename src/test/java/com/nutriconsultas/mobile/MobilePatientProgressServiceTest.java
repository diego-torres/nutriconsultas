package com.nutriconsultas.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.clinical.exam.AnthropometricMeasurement;
import com.nutriconsultas.clinical.exam.AnthropometricMeasurementRepository;
import com.nutriconsultas.clinical.exam.anthropometric.Circumferences;
import com.nutriconsultas.mobile.dto.PatientProgressSnapshotDto;
import com.nutriconsultas.mobile.dto.ProgressMeasurementsDto;
import com.nutriconsultas.paciente.NivelPeso;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteAvatarCatalog;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.metrics.BodyMetricRecord;
import com.nutriconsultas.paciente.metrics.BodyMetricRecordRepository;
import com.nutriconsultas.paciente.metrics.BodyMetricRecordService;
import com.nutriconsultas.paciente.metrics.BodyMetricSource;

@ExtendWith(MockitoExtension.class)
class MobilePatientProgressServiceTest {

	@InjectMocks
	private MobilePatientProgressService service;

	@Mock
	private BodyMetricRecordService bodyMetricRecordService;

	@Mock
	private BodyMetricRecordRepository bodyMetricRecordRepository;

	@Mock
	private AnthropometricMeasurementRepository anthropometricMeasurementRepository;

	@Mock
	private PacienteRepository pacienteRepository;

	@Test
	void getSnapshot_computesDeltasAndLabelsFromRecentRecords() {
		final Paciente paciente = samplePaciente();
		final BodyMetricRecord latest = metricRecord(2L, 70.0, 1.70, 24.2, NivelPeso.NORMAL, 22.0, null,
				Date.from(Instant.parse("2026-06-01T10:00:00Z")));
		final BodyMetricRecord previous = metricRecord(1L, 72.0, 1.70, 24.9, NivelPeso.ALTO, 23.0, null,
				Date.from(Instant.parse("2026-05-01T10:00:00Z")));
		final AnthropometricMeasurement measurement = new AnthropometricMeasurement();
		measurement.setCircumferences(new Circumferences());
		measurement.getCircumferences().setWaistCircumference(82.0);
		measurement.getCircumferences().setHipCircumference(98.0);

		when(pacienteRepository.findById(5L)).thenReturn(Optional.of(paciente));
		when(bodyMetricRecordRepository.findTop2ByPacienteIdOrderByRecordedAtDescIdDesc(5L))
			.thenReturn(List.of(latest, previous));
		when(anthropometricMeasurementRepository.findFirstByPacienteIdOrderByMeasurementDateTimeDescIdDesc(5L))
			.thenReturn(Optional.of(measurement));

		final PatientProgressSnapshotDto snapshot = service.getSnapshot(5L);

		verify(bodyMetricRecordService).ensureBackfilled(5L);
		assertThat(snapshot.weightKg()).isEqualTo(70.0);
		assertThat(snapshot.bmi()).isEqualTo(24.2);
		assertThat(snapshot.nivelPeso()).isEqualTo(NivelPeso.NORMAL);
		assertThat(snapshot.imcLabel()).isEqualTo("Normal");
		assertThat(snapshot.bmr()).isEqualTo(1500.0);
		assertThat(snapshot.bodyFatPercentage()).isEqualTo(22.0);
		assertThat(snapshot.deltaPeso()).isEqualTo(-2.0);
		assertThat(snapshot.deltaImc()).isCloseTo(-0.7, org.assertj.core.data.Offset.offset(0.0001));
		assertThat(snapshot.circumferences().waistCm()).isEqualTo(82.0);
		assertThat(snapshot.circumferences().hipCm()).isEqualTo(98.0);
		assertThat(snapshot.avatarId()).isEqualTo(PacienteAvatarCatalog.DEFAULT_MALE_ID);
		assertThat(snapshot.avatarUrl()).isEqualTo("/sbadmin/img/paciente-avatars/avatar_1.png");
	}

	@Test
	void getSnapshot_prefersBodyFatPercentageOverIndex() {
		final Paciente paciente = samplePaciente();
		final BodyMetricRecord latest = metricRecord(2L, 70.0, 1.70, 24.2, NivelPeso.NORMAL, 19.0, 21.5,
				Date.from(Instant.parse("2026-06-01T10:00:00Z")));

		when(pacienteRepository.findById(5L)).thenReturn(Optional.of(paciente));
		when(bodyMetricRecordRepository.findTop2ByPacienteIdOrderByRecordedAtDescIdDesc(5L))
			.thenReturn(List.of(latest));
		when(anthropometricMeasurementRepository.findFirstByPacienteIdOrderByMeasurementDateTimeDescIdDesc(5L))
			.thenReturn(Optional.empty());

		final PatientProgressSnapshotDto snapshot = service.getSnapshot(5L);

		assertThat(snapshot.bodyFatPercentage()).isEqualTo(21.5);
		assertThat(snapshot.deltaPeso()).isNull();
	}

	@Test
	void listMeasurements_returnsAscendingSeriesWithCircumferences() {
		final BodyMetricRecord older = metricRecord(1L, 72.0, 1.70, 24.9, NivelPeso.ALTO, 23.0, null,
				Date.from(Instant.parse("2026-05-01T10:00:00Z")));
		final BodyMetricRecord latest = metricRecord(2L, 70.0, 1.70, 24.2, NivelPeso.NORMAL, 19.0, 21.5,
				Date.from(Instant.parse("2026-06-01T10:00:00Z")));
		final AnthropometricMeasurement measurement = new AnthropometricMeasurement();
		measurement.setId(2L);
		measurement.setCircumferences(new Circumferences());
		measurement.getCircumferences().setWaistCircumference(82.0);
		measurement.getCircumferences().setHipCircumference(98.0);

		when(bodyMetricRecordRepository.countByPacienteId(5L)).thenReturn(2L);
		when(bodyMetricRecordRepository.findByPacienteIdOrderByRecordedAtAscIdAsc(eq(5L), any(Pageable.class)))
			.thenReturn(List.of(older, latest));
		when(anthropometricMeasurementRepository.findByPacienteIdAndIdIn(5L, List.of(1L, 2L)))
			.thenReturn(List.of(measurement));

		final ProgressMeasurementsDto series = service.listMeasurements(5L, null, null, null);

		verify(bodyMetricRecordService).ensureBackfilled(5L);
		assertThat(series.count()).isEqualTo(2);
		assertThat(series.truncated()).isFalse();
		assertThat(series.measurements()).extracting("weightKg").containsExactly(72.0, 70.0);
		assertThat(series.measurements().get(1).bodyFatPercentage()).isEqualTo(21.5);
		assertThat(series.measurements().get(1).circumferences().waistCm()).isEqualTo(82.0);
	}

	@Test
	void listMeasurements_truncatesBeyondMaxRowsCap() {
		when(bodyMetricRecordRepository.countByPacienteId(5L)).thenReturn(400L);
		when(bodyMetricRecordRepository.findByPacienteIdOrderByRecordedAtAscIdAsc(eq(5L), any(Pageable.class)))
			.thenReturn(List.of(metricRecord(1L, 70.0, 1.70, 24.2, NivelPeso.NORMAL, null, 22.0,
					Date.from(Instant.parse("2026-06-01T10:00:00Z")))));
		when(anthropometricMeasurementRepository.findByPacienteIdAndIdIn(eq(5L), any())).thenReturn(List.of());

		final ProgressMeasurementsDto series = service.listMeasurements(5L, null, null, 500);

		assertThat(series.truncated()).isTrue();
	}

	@Test
	void listMeasurements_rejectsInvalidDateRange() {
		final Instant from = Instant.parse("2026-06-02T00:00:00Z");
		final Instant to = Instant.parse("2026-06-01T00:00:00Z");

		assertThatThrownBy(() -> service.listMeasurements(5L, from, to, null))
			.isInstanceOf(ResponseStatusException.class);
	}

	private static Paciente samplePaciente() {
		final Paciente paciente = new Paciente();
		paciente.setId(5L);
		paciente.setBmr(1500.0);
		paciente.setGender("M");
		return paciente;
	}

	private static BodyMetricRecord metricRecord(final Long id, final Double weight, final Double height,
			final Double imc, final NivelPeso nivelPeso, final Double bodyFatIndex, final Double bodyFatPercentage,
			final Date recordedAt) {
		final BodyMetricRecord record = new BodyMetricRecord();
		record.setId(id);
		record.setSource(BodyMetricSource.ANTHROPOMETRIC);
		record.setSourceId(id);
		record.setWeight(weight);
		record.setHeight(height);
		record.setImc(imc);
		record.setNivelPeso(nivelPeso);
		record.setBodyFatIndex(bodyFatIndex);
		record.setBodyFatPercentage(bodyFatPercentage);
		record.setRecordedAt(recordedAt);
		return record;
	}

}
