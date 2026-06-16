package com.nutriconsultas.paciente.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.calendar.CalendarEventRepository;
import com.nutriconsultas.charts.ChartResponse;
import com.nutriconsultas.clinical.exam.AnthropometricMeasurement;
import com.nutriconsultas.clinical.exam.AnthropometricMeasurementRepository;
import com.nutriconsultas.clinical.exam.ClinicalExamRepository;
import com.nutriconsultas.clinical.exam.anthropometric.BodyMass;
import com.nutriconsultas.clinical.exam.anthropometric.MetodoObtencionComposicionCorporal;
import com.nutriconsultas.paciente.NivelPeso;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;

@ExtendWith(MockitoExtension.class)
class BodyMetricRecordServiceTest {

	@InjectMocks
	private BodyMetricRecordServiceImpl service;

	@Mock
	private BodyMetricRecordRepository repository;

	@Mock
	private CalendarEventRepository calendarEventRepository;

	@Mock
	private AnthropometricMeasurementRepository anthropometricMeasurementRepository;

	@Mock
	private ClinicalExamRepository clinicalExamRepository;

	@Mock
	private PacienteRepository pacienteRepository;

	private Paciente paciente;

	@BeforeEach
	void setUp() {
		paciente = new Paciente();
		paciente.setId(1L);
		paciente.setUserId("user-1");
	}

	@Test
	void buildChartResponsePrefersBodyFatPercentageOverIndex() {
		final BodyMetricRecord record = new BodyMetricRecord();
		record.setRecordedAt(new Date());
		record.setBodyFatPercentage(21.0);
		record.setBodyFatIndex(25.0);

		when(repository.existsByPacienteId(1L)).thenReturn(true);
		when(repository.findByPacienteIdOrderByRecordedAtAsc(1L)).thenReturn(List.of(record));

		final ChartResponse response = service.buildChartResponse(1L);

		@SuppressWarnings("unchecked")
		final List<Double> fatData = (List<Double>) response.getData().get("grasaCorporal");
		assertThat(fatData).containsExactly(21.0);
	}

	@Test
	void syncFromAnthropometricPersistsBodyFatPercentage() {
		final AnthropometricMeasurement measurement = new AnthropometricMeasurement();
		measurement.setId(10L);
		measurement.setPaciente(paciente);
		measurement.setMeasurementDateTime(new Date());
		measurement.setPorcentajeGrasaCorporal(20.5);
		measurement.setIndiceGrasaCorporal(20.5);
		measurement.setMetodoObtencion(MetodoObtencionComposicionCorporal.BIOIMPEDANCIA);

		when(repository.findBySourceAndSourceId(BodyMetricSource.ANTHROPOMETRIC, 10L)).thenReturn(Optional.empty());
		when(repository.save(any(BodyMetricRecord.class))).thenAnswer(invocation -> {
			final BodyMetricRecord saved = invocation.getArgument(0);
			saved.setId(99L);
			return saved;
		});

		service.syncFromAnthropometric(measurement);

		final ArgumentCaptor<BodyMetricRecord> captor = ArgumentCaptor.forClass(BodyMetricRecord.class);
		verify(repository).save(captor.capture());
		assertThat(captor.getValue().getBodyFatPercentage()).isEqualTo(20.5);
		assertThat(captor.getValue().getBodyFatIndex()).isEqualTo(20.5);
		assertThat(captor.getValue().getMetodoObtencionComposicion())
			.isEqualTo(MetodoObtencionComposicionCorporal.BIOIMPEDANCIA);
	}

	@Test
	void syncFromAnthropometricPersistsImcAndNivelPeso() {
		final AnthropometricMeasurement measurement = new AnthropometricMeasurement();
		measurement.setId(10L);
		measurement.setPaciente(paciente);
		measurement.setMeasurementDateTime(new Date());
		final BodyMass bodyMass = new BodyMass();
		bodyMass.setWeight(70.0);
		bodyMass.setHeight(1.75);
		bodyMass.setImc(22.86);
		bodyMass.setNivelPeso(NivelPeso.NORMAL);
		measurement.setBodyMass(bodyMass);

		when(repository.findBySourceAndSourceId(BodyMetricSource.ANTHROPOMETRIC, 10L)).thenReturn(Optional.empty());
		when(repository.save(any(BodyMetricRecord.class))).thenAnswer(invocation -> {
			final BodyMetricRecord saved = invocation.getArgument(0);
			saved.setId(99L);
			return saved;
		});

		service.syncFromAnthropometric(measurement);

		final ArgumentCaptor<BodyMetricRecord> captor = ArgumentCaptor.forClass(BodyMetricRecord.class);
		verify(repository).save(captor.capture());
		final BodyMetricRecord record = captor.getValue();
		assertThat(record.getImc()).isEqualTo(22.86);
		assertThat(record.getNivelPeso()).isEqualTo(NivelPeso.NORMAL);
		assertThat(record.getSource()).isEqualTo(BodyMetricSource.ANTHROPOMETRIC);
		assertThat(record.getSourceId()).isEqualTo(10L);
	}

	@Test
	void ensureBackfilledImportsConsultationsAndAnthropometrics() {
		final CalendarEvent consultation = new CalendarEvent();
		consultation.setId(1L);
		consultation.setPaciente(paciente);
		consultation.setEventDateTime(new Date());
		consultation.setImc(24.0);

		final AnthropometricMeasurement measurement = new AnthropometricMeasurement();
		measurement.setId(2L);
		measurement.setPaciente(paciente);
		measurement.setMeasurementDateTime(new Date());
		measurement.setImc(23.0);

		when(repository.existsByPacienteId(1L)).thenReturn(false);
		when(calendarEventRepository.findByPacienteId(1L)).thenReturn(List.of(consultation));
		when(anthropometricMeasurementRepository.findByPacienteId(1L)).thenReturn(List.of(measurement));
		when(clinicalExamRepository.findByPacienteId(1L)).thenReturn(new ArrayList<>());
		when(repository.findBySourceAndSourceId(any(), any())).thenReturn(Optional.empty());
		when(repository.save(any(BodyMetricRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.ensureBackfilled(1L);

		verify(repository, org.mockito.Mockito.times(2)).save(any(BodyMetricRecord.class));
		verify(calendarEventRepository).findByPacienteId(1L);
		verify(anthropometricMeasurementRepository).findByPacienteId(1L);
	}

	@Test
	void ensureBackfilledSkipsWhenRecordsAlreadyExist() {
		when(repository.existsByPacienteId(1L)).thenReturn(true);

		service.ensureBackfilled(1L);

		verify(calendarEventRepository, never()).findByPacienteId(1L);
	}

	@Test
	void buildChartResponseIncludesAnthropometricImcTimeline() {
		final BodyMetricRecord consultationRecord = new BodyMetricRecord();
		consultationRecord.setRecordedAt(new Date(1_000L));
		consultationRecord.setImc(24.0);

		final BodyMetricRecord anthropometricRecord = new BodyMetricRecord();
		anthropometricRecord.setRecordedAt(new Date(2_000L));
		anthropometricRecord.setImc(23.0);

		when(repository.existsByPacienteId(1L)).thenReturn(true);
		when(repository.findByPacienteIdOrderByRecordedAtAsc(1L))
			.thenReturn(List.of(consultationRecord, anthropometricRecord));

		final ChartResponse response = service.buildChartResponse(1L);

		@SuppressWarnings("unchecked")
		final List<Double> imcData = (List<Double>) response.getData().get("imc");
		assertThat(imcData).containsExactly(24.0, 23.0);
	}

	@Test
	void removeSourceAndRefreshPatientUsesPreviousRecord() {
		final BodyMetricRecord remainingRecord = new BodyMetricRecord();
		remainingRecord.setWeight(68.0);
		remainingRecord.setHeight(1.70);
		remainingRecord.setImc(23.53);
		remainingRecord.setNivelPeso(NivelPeso.NORMAL);

		when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
		when(repository.findFirstByPacienteIdOrderByRecordedAtDescIdDesc(1L)).thenReturn(Optional.of(remainingRecord));
		when(pacienteRepository.save(any(Paciente.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.removeSourceAndRefreshPatient(BodyMetricSource.ANTHROPOMETRIC, 10L, 1L);

		verify(repository).deleteBySourceAndSourceId(BodyMetricSource.ANTHROPOMETRIC, 10L);
		final ArgumentCaptor<Paciente> captor = ArgumentCaptor.forClass(Paciente.class);
		verify(pacienteRepository).save(captor.capture());
		assertThat(captor.getValue().getImc()).isEqualTo(23.53);
		assertThat(captor.getValue().getNivelPeso()).isEqualTo(NivelPeso.NORMAL);
	}

	@Test
	void removeSourceAndRefreshPatientClearsPatientWhenNoRecordsRemain() {
		when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
		paciente.setImc(25.0);
		paciente.setPeso(80.0);
		paciente.setEstatura(1.75);
		paciente.setNivelPeso(NivelPeso.ALTO);
		when(repository.findFirstByPacienteIdOrderByRecordedAtDescIdDesc(1L)).thenReturn(Optional.empty());
		when(pacienteRepository.save(any(Paciente.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.removeSourceAndRefreshPatient(BodyMetricSource.CONSULTATION, 5L, 1L);

		final ArgumentCaptor<Paciente> captor = ArgumentCaptor.forClass(Paciente.class);
		verify(pacienteRepository).save(captor.capture());
		assertThat(captor.getValue().getImc()).isNull();
		assertThat(captor.getValue().getPeso()).isNull();
		assertThat(captor.getValue().getEstatura()).isNull();
		assertThat(captor.getValue().getNivelPeso()).isNull();
	}

}
