package com.nutriconsultas.reports;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.nutriconsultas.clinical.exam.AnthropometricMeasurement;
import com.nutriconsultas.paciente.metrics.BodyMetricRecord;

/**
 * Builds progress indicator charts for patient PDF reports.
 */
public final class PatientReportProgressChartBuilder {

	private static final String EMPTY_MESSAGE = "Al registrar mediciones antropométricas se habilitará el seguimiento de este indicador.";

	private PatientReportProgressChartBuilder() {
	}

	public static List<PatientReportChart> build(final List<BodyMetricRecord> records,
			final List<AnthropometricMeasurement> measurements) {
		final List<PatientReportChart> charts = new ArrayList<>();
		charts.add(buildFromRecords("Peso", "kg", records, BodyMetricRecord::getWeight, "#4e73df"));
		charts.add(buildFromRecords("IMC", "", records, BodyMetricRecord::getImc, "#1cc88a"));
		charts.add(buildFromRecords("% Grasa corporal", "%", records, PatientReportProgressChartBuilder::resolveBodyFat,
				"#e67e22"));
		charts.add(buildFromRecords("GET", "kcal/día", records, BodyMetricRecord::getGetKcal, "#ff6b35"));
		charts.add(buildFromRecords("TMB (BMR)", "kcal/día", records, BodyMetricRecord::getBmr, "#e74a3b"));
		charts.add(buildFromRecords("Total (GET+TEF)", "kcal/día", records, BodyMetricRecord::getTotalAdjustedKcal,
				"#28a745"));
		charts.add(buildFromMeasurements("% Masa muscular", "%", measurements,
				AnthropometricMeasurement::getPorcentajeMasaMuscular, "#6f42c1"));
		charts.add(buildFromMeasurements("Masa ósea", "kg", measurements, AnthropometricMeasurement::getMasaOseaKg,
				"#adb5bd"));
		return charts;
	}

	private static Double resolveBodyFat(final BodyMetricRecord record) {
		if (record.getBodyFatPercentage() != null) {
			return record.getBodyFatPercentage();
		}
		return record.getBodyFatIndex();
	}

	private static PatientReportChart buildFromRecords(final String title, final String unit,
			final List<BodyMetricRecord> records, final Function<BodyMetricRecord, Double> valueExtractor,
			final String color) {
		final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");
		final List<String> labels = new ArrayList<>();
		final List<Double> values = new ArrayList<>();

		final List<BodyMetricRecord> sortedRecords = records.stream()
			.sorted(Comparator.comparing(BodyMetricRecord::getRecordedAt))
			.collect(Collectors.toList());

		for (final BodyMetricRecord record : sortedRecords) {
			final Double value = valueExtractor.apply(record);
			if (value != null && record.getRecordedAt() != null) {
				labels.add(dateFormat.format(record.getRecordedAt()));
				values.add(value);
			}
		}

		return buildChart(title, unit, labels, values, color);
	}

	private static PatientReportChart buildFromMeasurements(final String title, final String unit,
			final List<AnthropometricMeasurement> measurements,
			final Function<AnthropometricMeasurement, Double> valueExtractor, final String color) {
		final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");
		final List<String> labels = new ArrayList<>();
		final List<Double> values = new ArrayList<>();

		final List<AnthropometricMeasurement> sortedMeasurements = measurements.stream()
			.filter(measurement -> measurement.getMeasurementDateTime() != null)
			.sorted(Comparator.comparing(AnthropometricMeasurement::getMeasurementDateTime))
			.collect(Collectors.toList());

		for (final AnthropometricMeasurement measurement : sortedMeasurements) {
			final Double value = valueExtractor.apply(measurement);
			if (value != null) {
				labels.add(dateFormat.format(measurement.getMeasurementDateTime()));
				values.add(value);
			}
		}

		return buildChart(title, unit, labels, values, color);
	}

	private static PatientReportChart buildChart(final String title, final String unit, final List<String> labels,
			final List<Double> values, final String color) {
		final PatientReportChart chart = new PatientReportChart();
		chart.setTitle(title);
		chart.setUnit(unit);
		chart.setEmptyMessage(EMPTY_MESSAGE);
		if (values.isEmpty()) {
			chart.setHasData(false);
			chart.setSvgMarkup(null);
		}
		else {
			chart.setHasData(true);
			chart.setSvgMarkup(ReportLineChartRenderer.render(labels, values, color, unit));
		}
		return chart;
	}

	public static List<BodyMetricRecord> filterRecordsByDateRange(final List<BodyMetricRecord> records,
			final Date startDate, final Date endDate) {
		if (startDate == null && endDate == null) {
			return records;
		}
		return records.stream().filter(record -> {
			final Date recordedAt = record.getRecordedAt();
			return recordedAt != null && (startDate == null || !recordedAt.before(startDate))
					&& (endDate == null || !recordedAt.after(endDate));
		}).collect(Collectors.toList());
	}

}
