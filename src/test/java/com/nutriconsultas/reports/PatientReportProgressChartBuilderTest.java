package com.nutriconsultas.reports;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.nutriconsultas.clinical.exam.AnthropometricMeasurement;
import com.nutriconsultas.clinical.exam.anthropometric.BodyComposition;
import com.nutriconsultas.paciente.metrics.BodyMetricRecord;

class PatientReportProgressChartBuilderTest {

	@Test
	void buildCreatesChartsWithDataAndEmptyStates() {
		final BodyMetricRecord weightRecord = new BodyMetricRecord();
		weightRecord.setRecordedAt(new Date());
		weightRecord.setWeight(72.0);

		final BodyMetricRecord imcRecord = new BodyMetricRecord();
		imcRecord.setRecordedAt(new Date());
		imcRecord.setImc(23.5);

		final AnthropometricMeasurement measurement = new AnthropometricMeasurement();
		measurement.setMeasurementDateTime(new Date());
		final BodyComposition composition = new BodyComposition();
		composition.setPorcentajeMasaMuscular(42.0);
		measurement.setBodyComposition(composition);

		final List<PatientReportChart> charts = PatientReportProgressChartBuilder
			.build(List.of(weightRecord, imcRecord), List.of(measurement));

		assertThat(charts).hasSize(8);

		final PatientReportChart weightChart = charts.stream()
			.filter(chart -> "Peso".equals(chart.getTitle()))
			.findFirst()
			.orElseThrow();
		assertThat(weightChart.isHasData()).isTrue();
		assertThat(weightChart.getSvgMarkup()).contains("<svg");

		final PatientReportChart getChart = charts.stream()
			.filter(chart -> "GET".equals(chart.getTitle()))
			.findFirst()
			.orElseThrow();
		assertThat(getChart.isHasData()).isFalse();
		assertThat(getChart.getEmptyMessage()).contains("antropométricas");

		final PatientReportChart muscleChart = charts.stream()
			.filter(chart -> "% Masa muscular".equals(chart.getTitle()))
			.findFirst()
			.orElseThrow();
		assertThat(muscleChart.isHasData()).isTrue();
	}

	@Test
	void filterRecordsByDateRangeKeepsRecordsInRange() {
		final long now = System.currentTimeMillis();
		final BodyMetricRecord inRange = new BodyMetricRecord();
		inRange.setRecordedAt(new Date(now - 5L * 24 * 60 * 60 * 1000));

		final BodyMetricRecord outOfRange = new BodyMetricRecord();
		outOfRange.setRecordedAt(new Date(now - 40L * 24 * 60 * 60 * 1000));

		final Date startDate = new Date(now - 10L * 24 * 60 * 60 * 1000);
		final Date endDate = new Date(now);

		final List<BodyMetricRecord> filtered = PatientReportProgressChartBuilder
			.filterRecordsByDateRange(List.of(inRange, outOfRange), startDate, endDate);

		assertThat(filtered).containsExactly(inRange);
	}

	@Test
	void buildReturnsAllEmptyWhenNoData() {
		final List<PatientReportChart> charts = PatientReportProgressChartBuilder.build(new ArrayList<>(),
				new ArrayList<>());

		assertThat(charts).hasSize(8);
		assertThat(charts).allMatch(chart -> !chart.isHasData());
	}

}
