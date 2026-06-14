package com.nutriconsultas.mobile;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.nutriconsultas.clinical.exam.AnthropometricMeasurement;
import com.nutriconsultas.clinical.exam.AnthropometricMeasurementRepository;
import com.nutriconsultas.clinical.exam.anthropometric.Circumferences;
import com.nutriconsultas.mobile.dto.PatientProgressSnapshotDto;
import com.nutriconsultas.paciente.NivelPeso;
import com.nutriconsultas.paciente.Paciente;
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

	private static Paciente samplePaciente() {
		final Paciente paciente = new Paciente();
		paciente.setId(5L);
		paciente.setBmr(1500.0);
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
