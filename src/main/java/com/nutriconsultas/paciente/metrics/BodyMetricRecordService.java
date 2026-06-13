package com.nutriconsultas.paciente.metrics;

import java.util.Optional;

import org.springframework.lang.NonNull;

import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.charts.ChartResponse;
import com.nutriconsultas.clinical.exam.AnthropometricMeasurement;
import com.nutriconsultas.clinical.exam.ClinicalExam;

public interface BodyMetricRecordService {

	void syncFromConsultation(@NonNull CalendarEvent event);

	void syncFromAnthropometric(@NonNull AnthropometricMeasurement measurement);

	void syncFromClinicalExam(@NonNull ClinicalExam exam);

	void ensureBackfilled(@NonNull Long pacienteId);

	void removeSourceAndRefreshPatient(@NonNull BodyMetricSource source, @NonNull Long sourceId,
			@NonNull Long pacienteId);

	Optional<BodyMetricRecord> findLatestByPacienteId(@NonNull Long pacienteId);

	ChartResponse buildChartResponse(@NonNull Long pacienteId);

}
