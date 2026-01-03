package com.nutriconsultas.reports;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.calendar.CalendarEventService;
import com.nutriconsultas.clinical.exam.AnthropometricMeasurement;
import com.nutriconsultas.clinical.exam.AnthropometricMeasurementService;
import com.nutriconsultas.clinical.exam.ClinicalExam;
import com.nutriconsultas.clinical.exam.ClinicalExamService;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteDieta;
import com.nutriconsultas.paciente.PacienteDietaRepository;
import com.nutriconsultas.paciente.PacienteService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for generating patient progress reports in PDF format.
 *
 * <p>
 * This service generates comprehensive patient progress reports including:
 * <ul>
 * <li>Patient demographics and medical history</li>
 * <li>Weight/BMI trend data over time</li>
 * <li>Consultation history with key metrics</li>
 * <li>Current dietary plan summary</li>
 * <li>Progress notes and recommendations</li>
 * </ul>
 *
 * <p>
 * Reports can be filtered by date range to show progress over specific periods.
 *
 * @see Paciente
 * @see CalendarEvent
 * @see AnthropometricMeasurement
 * @see ClinicalExam
 */
@Service
@Slf4j
public class PatientReportService {

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class WeightBmiDataPoint {

		private Date date;

		private Double weight;

		private Double bmi;

		private String source;

	}

	@Autowired
	private TemplateEngine templateEngine;

	@Autowired
	private PacienteService pacienteService;

	@Autowired
	private CalendarEventService calendarEventService;

	@Autowired
	private AnthropometricMeasurementService anthropometricMeasurementService;

	@Autowired
	private ClinicalExamService clinicalExamService;

	@Autowired
	private PacienteDietaRepository pacienteDietaRepository;

	/**
	 * Generates a PDF progress report for a patient.
	 * @param pacienteId the ID of the patient
	 * @param userId the user ID to verify patient ownership
	 * @param startDate optional start date for filtering data (null for all data)
	 * @param endDate optional end date for filtering data (null for all data)
	 * @return PDF document as byte array
	 * @throws IllegalArgumentException if patient with the given ID is not found or
	 * doesn't belong to the user
	 * @throws IllegalStateException if PDF generation fails
	 */
	public byte[] generateReport(@NonNull final Long pacienteId, @NonNull final String userId, final Date startDate,
			final Date endDate) {
		log.info("Generating progress report for patient id: {} (user: {})", pacienteId, userId);

		// Verify patient ownership
		final Paciente paciente = pacienteService.findByIdAndUserId(pacienteId, userId);
		if (paciente == null) {
			throw new IllegalArgumentException("Patient with id " + pacienteId + " not found or access denied");
		}

		// Collect all data for the report
		final List<CalendarEvent> consultations = getConsultations(pacienteId, startDate, endDate);
		final List<AnthropometricMeasurement> measurements = getMeasurements(pacienteId, startDate, endDate);
		final List<ClinicalExam> exams = getExams(pacienteId, startDate, endDate);
		final List<PacienteDieta> dietaryPlans = getDietaryPlans(pacienteId);
		final List<WeightBmiDataPoint> weightBmiTrend = buildWeightBmiTrend(consultations, measurements, exams);

		// Prepare context for Thymeleaf template
		final Context context = new Context();
		context.setVariable("paciente", paciente);
		context.setVariable("consultations", consultations);
		context.setVariable("measurements", measurements);
		context.setVariable("exams", exams);
		context.setVariable("dietaryPlans", dietaryPlans);
		context.setVariable("weightBmiTrend", weightBmiTrend);
		context.setVariable("startDate", startDate);
		context.setVariable("endDate", endDate);
		context.setVariable("reportDate", new Date());

		// Render Thymeleaf template to HTML
		final String html = templateEngine.process("sbadmin/reports/patient-progress", context);

		// Convert HTML to PDF using Flying Saucer
		return htmlToPdf(html);
	}

	private List<CalendarEvent> getConsultations(@NonNull final Long pacienteId, final Date startDate,
			final Date endDate) {
		final List<CalendarEvent> allConsultations = calendarEventService.findByPacienteId(pacienteId);
		return filterByDateRange(allConsultations, startDate, endDate,
				event -> event.getEventDateTime() != null ? event.getEventDateTime() : null);
	}

	private List<AnthropometricMeasurement> getMeasurements(@NonNull final Long pacienteId, final Date startDate,
			final Date endDate) {
		final List<AnthropometricMeasurement> allMeasurements = anthropometricMeasurementService
			.findByPacienteId(pacienteId);
		return filterByDateRange(allMeasurements, startDate, endDate,
				measurement -> measurement.getMeasurementDateTime() != null ? measurement.getMeasurementDateTime()
						: null);
	}

	private List<ClinicalExam> getExams(@NonNull final Long pacienteId, final Date startDate, final Date endDate) {
		final List<ClinicalExam> allExams = clinicalExamService.findByPacienteId(pacienteId);
		return filterByDateRange(allExams, startDate, endDate,
				exam -> exam.getExamDateTime() != null ? exam.getExamDateTime() : null);
	}

	private List<PacienteDieta> getDietaryPlans(@NonNull final Long pacienteId) {
		return pacienteDietaRepository.findByPacienteIdOrderByStartDateDesc(pacienteId);
	}

	private <T> List<T> filterByDateRange(final List<T> items, final Date startDate, final Date endDate,
			final java.util.function.Function<T, Date> dateExtractor) {
		if (startDate == null && endDate == null) {
			return items;
		}
		return items.stream().filter(item -> {
			final Date itemDate = dateExtractor.apply(item);
			if (itemDate == null) {
				return false;
			}
			if (startDate != null && itemDate.before(startDate)) {
				return false;
			}
			if (endDate != null && itemDate.after(endDate)) {
				return false;
			}
			return true;
		}).collect(Collectors.toList());
	}

	private List<WeightBmiDataPoint> buildWeightBmiTrend(final List<CalendarEvent> consultations,
			final List<AnthropometricMeasurement> measurements, final List<ClinicalExam> exams) {
		final List<WeightBmiDataPoint> trend = new ArrayList<>();

		// Add data from consultations
		for (final CalendarEvent event : consultations) {
			if (event.getEventDateTime() != null && (event.getPeso() != null || event.getImc() != null)) {
				final WeightBmiDataPoint point = new WeightBmiDataPoint();
				point.setDate(event.getEventDateTime());
				point.setWeight(event.getPeso());
				point.setBmi(event.getImc());
				point.setSource("Consulta");
				trend.add(point);
			}
		}

		// Add data from measurements
		for (final AnthropometricMeasurement measurement : measurements) {
			if (measurement.getMeasurementDateTime() != null
					&& (measurement.getPeso() != null || measurement.getBodyMass() != null)) {
				final WeightBmiDataPoint point = new WeightBmiDataPoint();
				point.setDate(measurement.getMeasurementDateTime());
				point.setWeight(measurement.getPeso());
				if (measurement.getBodyMass() != null) {
					point.setBmi(measurement.getBodyMass().getImc());
				}
				point.setSource("Medición Antropométrica");
				trend.add(point);
			}
		}

		// Add data from exams
		for (final ClinicalExam exam : exams) {
			if (exam.getExamDateTime() != null && exam.getPeso() != null) {
				final WeightBmiDataPoint point = new WeightBmiDataPoint();
				point.setDate(exam.getExamDateTime());
				point.setWeight(exam.getPeso());
				point.setSource("Examen Clínico");
				trend.add(point);
			}
		}

		// Sort by date
		trend.sort(Comparator.comparing(WeightBmiDataPoint::getDate));

		return trend;
	}

	private byte[] htmlToPdf(final String html) {
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			final ITextRenderer renderer = new ITextRenderer();
			renderer.setDocumentFromString(html);
			renderer.layout();
			renderer.createPDF(outputStream);
			return outputStream.toByteArray();
		}
		catch (final Exception e) {
			log.error("Error generating PDF", e);
			throw new IllegalStateException("Error generating PDF", e);
		}
	}

}
