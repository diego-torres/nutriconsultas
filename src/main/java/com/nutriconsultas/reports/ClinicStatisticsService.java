package com.nutriconsultas.reports;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.calendar.CalendarEventRepository;
import com.nutriconsultas.calendar.EventStatus;
import com.nutriconsultas.clinical.exam.AnthropometricMeasurement;
import com.nutriconsultas.clinical.exam.AnthropometricMeasurementRepository;
import com.nutriconsultas.clinical.exam.ClinicalExam;
import com.nutriconsultas.clinical.exam.ClinicalExamRepository;
import com.nutriconsultas.paciente.NivelPeso;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteDieta;
import com.nutriconsultas.paciente.PacienteDietaRepository;
import com.nutriconsultas.paciente.PacienteDietaStatus;
import com.nutriconsultas.paciente.PacienteRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for generating clinic statistics reports.
 *
 * <p>
 * Aggregates statistics across all patients for a specific user, including:
 * <ul>
 * <li>Total patients, consultations, dietary plans, and clinical exams</li>
 * <li>Patient demographics breakdown (gender, age groups, weight levels)</li>
 * <li>Consultation frequency trends</li>
 * <li>Most common nutritional conditions</li>
 * <li>Average weight/BMI changes</li>
 * <li>Monthly activity trends</li>
 * </ul>
 *
 * <p>
 * All statistics are filtered by userId to ensure multi-tenant data isolation.
 */
@Service
@Slf4j
public class ClinicStatisticsService {

	@Autowired
	private PacienteRepository pacienteRepository;

	@Autowired
	private CalendarEventRepository calendarEventRepository;

	@Autowired
	private PacienteDietaRepository pacienteDietaRepository;

	@Autowired
	private ClinicalExamRepository clinicalExamRepository;

	@Autowired
	private AnthropometricMeasurementRepository anthropometricMeasurementRepository;

	/**
	 * Generates comprehensive clinic statistics for a user.
	 * @param userId the user ID to filter statistics by
	 * @param startDate optional start date for filtering (null for all data)
	 * @param endDate optional end date for filtering (null for all data)
	 * @return ClinicStatistics object containing all aggregated statistics
	 */
	@Transactional(readOnly = true)
	public ClinicStatistics generateStatistics(@NonNull final String userId, final Date startDate, final Date endDate) {
		log.info("Generating clinic statistics for user: {} (date range: {} to {})", userId, startDate, endDate);

		// Get all patients for the user
		final List<Paciente> pacientes = pacienteRepository.findByUserId(userId);

		// Get all consultations, filtered by date range if provided
		final List<CalendarEvent> consultations = getConsultations(userId, startDate, endDate);

		// Get all dietary plans
		final List<PacienteDieta> dietaryPlans = getDietaryPlans(userId, startDate, endDate);

		// Get all clinical exams
		final List<ClinicalExam> clinicalExams = getClinicalExams(userId, startDate, endDate);

		// Get all anthropometric measurements
		final List<AnthropometricMeasurement> measurements = getMeasurements(userId, startDate, endDate);

		// Build statistics object
		final ClinicStatistics stats = new ClinicStatistics();

		// Set date range
		stats.setStartDate(startDate);
		stats.setEndDate(endDate);
		stats.setReportDate(new Date());

		// Summary metrics
		stats.setTotalPatients((long) pacientes.size());
		stats.setTotalConsultations((long) consultations.size());
		stats.setTotalDietaryPlans((long) dietaryPlans.size());
		stats.setTotalClinicalExams((long) clinicalExams.size());
		stats.setTotalAnthropometricMeasurements((long) measurements.size());

		// Active dietary plans
		final long activePlans = pacienteDietaRepository.countByUserIdAndStatus(userId, PacienteDietaStatus.ACTIVE);
		stats.setActiveDietaryPlans(activePlans);

		// Completed consultations
		final long completedConsultations = calendarEventRepository.countByUserIdAndStatus(userId,
				EventStatus.COMPLETED);
		stats.setCompletedConsultations(completedConsultations);

		// New patients in period
		if (startDate != null) {
			final long newPatients = pacientes.stream()
				.filter(p -> p.getRegistro() != null && !p.getRegistro().before(startDate)
						&& (endDate == null || !p.getRegistro().after(endDate)))
				.count();
			stats.setNewPatientsInPeriod(newPatients);
		}
		else {
			stats.setNewPatientsInPeriod((long) pacientes.size());
		}

		// Demographics
		stats.setGenderDistribution(calculateGenderDistribution(pacientes));
		stats.setAgeGroupDistribution(calculateAgeGroupDistribution(pacientes));
		stats.setWeightLevelDistribution(calculateWeightLevelDistribution(pacientes));

		// Consultation trends
		stats.setConsultationsByMonth(calculateConsultationsByMonth(consultations));
		stats.setConsultationsByStatus(calculateConsultationsByStatus(consultations));
		stats.setAverageConsultationsPerPatient(calculateAverageConsultationsPerPatient(pacientes, consultations));

		// Most common conditions
		stats.setConditionFrequency(calculateConditionFrequency(pacientes));

		// Weight/BMI metrics
		stats.setAverageWeight(calculateAverageWeight(pacientes));
		stats.setAverageBMI(calculateAverageBMI(pacientes));
		stats.setAverageWeightChange(calculateAverageWeightChange(consultations, measurements, clinicalExams));
		stats.setAverageBMIChange(calculateAverageBMIChange(consultations, measurements, clinicalExams));

		// Monthly trends
		stats.setMonthlyTrends(calculateMonthlyTrends(userId, startDate, endDate));

		log.info("Successfully generated clinic statistics for user: {}", userId);
		return stats;
	}

	private List<CalendarEvent> getConsultations(final String userId, final Date startDate, final Date endDate) {
		if (startDate != null && endDate != null) {
			return calendarEventRepository.findByUserIdAndDateRange(userId, startDate, endDate);
		}
		// Get all consultations for the user
		final List<Paciente> pacientes = pacienteRepository.findByUserId(userId);
		return pacientes.stream()
			.flatMap(p -> calendarEventRepository.findByPacienteId(p.getId()).stream())
			.collect(Collectors.toList());
	}

	private List<PacienteDieta> getDietaryPlans(final String userId, final Date startDate, final Date endDate) {
		if (startDate != null && endDate != null) {
			return pacienteDietaRepository.findByUserIdAndDateRange(userId, startDate, endDate);
		}
		return pacienteDietaRepository.findByUserId(userId);
	}

	private List<ClinicalExam> getClinicalExams(final String userId, final Date startDate, final Date endDate) {
		if (startDate != null && endDate != null) {
			return clinicalExamRepository.findByUserIdAndDateRange(userId, startDate, endDate);
		}
		// Get all clinical exams for the user
		final List<Paciente> pacientes = pacienteRepository.findByUserId(userId);
		return pacientes.stream()
			.flatMap(p -> clinicalExamRepository.findByPacienteId(p.getId()).stream())
			.collect(Collectors.toList());
	}

	private List<AnthropometricMeasurement> getMeasurements(final String userId, final Date startDate,
			final Date endDate) {
		if (startDate != null && endDate != null) {
			return anthropometricMeasurementRepository.findByUserIdAndDateRange(userId, startDate, endDate);
		}
		// Get all measurements for the user
		final List<Paciente> pacientes = pacienteRepository.findByUserId(userId);
		return pacientes.stream()
			.flatMap(p -> anthropometricMeasurementRepository.findByPacienteId(p.getId()).stream())
			.collect(Collectors.toList());
	}

	private Map<String, Long> calculateGenderDistribution(final List<Paciente> pacientes) {
		final Map<String, Long> distribution = new HashMap<>();
		distribution.put("M", 0L);
		distribution.put("F", 0L);
		distribution.put("Otro", 0L);
		distribution.put("No especificado", 0L);

		for (final Paciente paciente : pacientes) {
			final String gender = paciente.getGender();
			if (gender == null || gender.isEmpty()) {
				distribution.put("No especificado", distribution.get("No especificado") + 1);
			}
			else if ("M".equalsIgnoreCase(gender) || "MASCULINO".equalsIgnoreCase(gender)) {
				distribution.put("M", distribution.get("M") + 1);
			}
			else if ("F".equalsIgnoreCase(gender) || "FEMENINO".equalsIgnoreCase(gender)) {
				distribution.put("F", distribution.get("F") + 1);
			}
			else {
				distribution.put("Otro", distribution.get("Otro") + 1);
			}
		}

		return distribution;
	}

	private Map<String, Long> calculateAgeGroupDistribution(final List<Paciente> pacientes) {
		final Map<String, Long> distribution = new HashMap<>();
		distribution.put("0-5", 0L);
		distribution.put("6-12", 0L);
		distribution.put("13-18", 0L);
		distribution.put("19-30", 0L);
		distribution.put("31-50", 0L);
		distribution.put("51-65", 0L);
		distribution.put("65+", 0L);
		distribution.put("No especificado", 0L);

		final LocalDate now = LocalDate.now();
		for (final Paciente paciente : pacientes) {
			if (paciente.getDob() == null) {
				distribution.put("No especificado", distribution.get("No especificado") + 1);
				continue;
			}

			final LocalDate dob = paciente.getDob().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			final int age = (int) java.time.temporal.ChronoUnit.YEARS.between(dob, now);

			if (age < 0) {
				distribution.put("No especificado", distribution.get("No especificado") + 1);
			}
			else if (age <= 5) {
				distribution.put("0-5", distribution.get("0-5") + 1);
			}
			else if (age <= 12) {
				distribution.put("6-12", distribution.get("6-12") + 1);
			}
			else if (age <= 18) {
				distribution.put("13-18", distribution.get("13-18") + 1);
			}
			else if (age <= 30) {
				distribution.put("19-30", distribution.get("19-30") + 1);
			}
			else if (age <= 50) {
				distribution.put("31-50", distribution.get("31-50") + 1);
			}
			else if (age <= 65) {
				distribution.put("51-65", distribution.get("51-65") + 1);
			}
			else {
				distribution.put("65+", distribution.get("65+") + 1);
			}
		}

		return distribution;
	}

	private Map<String, Long> calculateWeightLevelDistribution(final List<Paciente> pacientes) {
		final Map<String, Long> distribution = new HashMap<>();
		distribution.put("BAJO", 0L);
		distribution.put("NORMAL", 0L);
		distribution.put("ALTO", 0L);
		distribution.put("SOBREPESO", 0L);
		distribution.put("No especificado", 0L);

		for (final Paciente paciente : pacientes) {
			final NivelPeso nivelPeso = paciente.getNivelPeso();
			if (nivelPeso == null) {
				distribution.put("No especificado", distribution.get("No especificado") + 1);
			}
			else {
				distribution.put(nivelPeso.name(), distribution.get(nivelPeso.name()) + 1);
			}
		}

		return distribution;
	}

	private Map<String, Long> calculateConsultationsByMonth(final List<CalendarEvent> consultations) {
		final Map<String, Long> byMonth = new HashMap<>();
		final java.text.SimpleDateFormat monthFormat = new java.text.SimpleDateFormat("yyyy-MM");

		for (final CalendarEvent consultation : consultations) {
			if (consultation.getEventDateTime() != null) {
				final String month = monthFormat.format(consultation.getEventDateTime());
				byMonth.put(month, byMonth.getOrDefault(month, 0L) + 1);
			}
		}

		return byMonth;
	}

	private Map<String, Long> calculateConsultationsByStatus(final List<CalendarEvent> consultations) {
		final Map<String, Long> byStatus = new HashMap<>();

		for (final CalendarEvent consultation : consultations) {
			final String status = consultation.getStatus() != null ? consultation.getStatus().name() : "UNKNOWN";
			byStatus.put(status, byStatus.getOrDefault(status, 0L) + 1);
		}

		return byStatus;
	}

	private Double calculateAverageConsultationsPerPatient(final List<Paciente> pacientes,
			final List<CalendarEvent> consultations) {
		if (pacientes.isEmpty()) {
			return 0.0;
		}
		return (double) consultations.size() / pacientes.size();
	}

	private Map<String, Long> calculateConditionFrequency(final List<Paciente> pacientes) {
		final Map<String, Long> frequency = new HashMap<>();

		for (final Paciente paciente : pacientes) {
			if (Boolean.TRUE.equals(paciente.getHipertension())) {
				frequency.put("Hipertensión", frequency.getOrDefault("Hipertensión", 0L) + 1);
			}
			if (Boolean.TRUE.equals(paciente.getDiabetes())) {
				frequency.put("Diabetes", frequency.getOrDefault("Diabetes", 0L) + 1);
			}
			if (Boolean.TRUE.equals(paciente.getHipotiroidismo())) {
				frequency.put("Hipotiroidismo", frequency.getOrDefault("Hipotiroidismo", 0L) + 1);
			}
			if (Boolean.TRUE.equals(paciente.getObesidad())) {
				frequency.put("Obesidad", frequency.getOrDefault("Obesidad", 0L) + 1);
			}
			if (Boolean.TRUE.equals(paciente.getAnemia())) {
				frequency.put("Anemia", frequency.getOrDefault("Anemia", 0L) + 1);
			}
			if (Boolean.TRUE.equals(paciente.getBulimia())) {
				frequency.put("Bulimia", frequency.getOrDefault("Bulimia", 0L) + 1);
			}
			if (Boolean.TRUE.equals(paciente.getAnorexia())) {
				frequency.put("Anorexia", frequency.getOrDefault("Anorexia", 0L) + 1);
			}
			if (Boolean.TRUE.equals(paciente.getEnfermedadesHepaticas())) {
				frequency.put("Enfermedades Hepáticas", frequency.getOrDefault("Enfermedades Hepáticas", 0L) + 1);
			}
		}

		return frequency;
	}

	private Double calculateAverageWeight(final List<Paciente> pacientes) {
		final List<Double> weights = pacientes.stream()
			.map(Paciente::getPeso)
			.filter(w -> w != null && w > 0)
			.collect(Collectors.toList());

		if (weights.isEmpty()) {
			return null;
		}

		return weights.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
	}

	private Double calculateAverageBMI(final List<Paciente> pacientes) {
		final List<Double> bmis = pacientes.stream()
			.map(Paciente::getImc)
			.filter(b -> b != null && b > 0)
			.collect(Collectors.toList());

		if (bmis.isEmpty()) {
			return null;
		}

		return bmis.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
	}

	private Double calculateAverageWeightChange(final List<CalendarEvent> consultations,
			final List<AnthropometricMeasurement> measurements, final List<ClinicalExam> exams) {
		final List<Double> weightChanges = new ArrayList<>();

		// Group weights by patient and calculate changes
		final Map<Long, List<Double>> weightsByPatient = new HashMap<>();

		// Add weights from consultations
		for (final CalendarEvent consultation : consultations) {
			if (consultation.getPaciente() != null && consultation.getPeso() != null
					&& consultation.getEventDateTime() != null) {
				final Long pacienteId = consultation.getPaciente().getId();
				weightsByPatient.computeIfAbsent(pacienteId, k -> new ArrayList<>()).add(consultation.getPeso());
			}
		}

		// Add weights from measurements
		for (final AnthropometricMeasurement measurement : measurements) {
			if (measurement.getPaciente() != null && measurement.getPeso() != null
					&& measurement.getMeasurementDateTime() != null) {
				final Long pacienteId = measurement.getPaciente().getId();
				weightsByPatient.computeIfAbsent(pacienteId, k -> new ArrayList<>()).add(measurement.getPeso());
			}
		}

		// Add weights from exams
		for (final ClinicalExam exam : exams) {
			if (exam.getPaciente() != null && exam.getPeso() != null && exam.getExamDateTime() != null) {
				final Long pacienteId = exam.getPaciente().getId();
				weightsByPatient.computeIfAbsent(pacienteId, k -> new ArrayList<>()).add(exam.getPeso());
			}
		}

		// Calculate changes for each patient
		for (final List<Double> weights : weightsByPatient.values()) {
			if (weights.size() >= 2) {
				weights.sort(Comparator.naturalOrder());
				final double change = weights.get(weights.size() - 1) - weights.get(0);
				weightChanges.add(change);
			}
		}

		if (weightChanges.isEmpty()) {
			return null;
		}

		return weightChanges.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
	}

	private Double calculateAverageBMIChange(final List<CalendarEvent> consultations,
			final List<AnthropometricMeasurement> measurements, final List<ClinicalExam> exams) {
		final List<Double> bmiChanges = new ArrayList<>();

		// Group BMIs by patient and calculate changes
		final Map<Long, List<Double>> bmisByPatient = new HashMap<>();

		// Add BMIs from consultations
		for (final CalendarEvent consultation : consultations) {
			if (consultation.getPaciente() != null && consultation.getImc() != null
					&& consultation.getEventDateTime() != null) {
				final Long pacienteId = consultation.getPaciente().getId();
				bmisByPatient.computeIfAbsent(pacienteId, k -> new ArrayList<>()).add(consultation.getImc());
			}
		}

		// Add BMIs from measurements
		for (final AnthropometricMeasurement measurement : measurements) {
			if (measurement.getPaciente() != null && measurement.getImc() != null
					&& measurement.getMeasurementDateTime() != null) {
				final Long pacienteId = measurement.getPaciente().getId();
				bmisByPatient.computeIfAbsent(pacienteId, k -> new ArrayList<>()).add(measurement.getImc());
			}
		}

		// Calculate changes for each patient
		for (final List<Double> bmis : bmisByPatient.values()) {
			if (bmis.size() >= 2) {
				bmis.sort(Comparator.naturalOrder());
				final double change = bmis.get(bmis.size() - 1) - bmis.get(0);
				bmiChanges.add(change);
			}
		}

		if (bmiChanges.isEmpty()) {
			return null;
		}

		return bmiChanges.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
	}

	private List<ClinicStatistics.MonthlyTrend> calculateMonthlyTrends(final String userId, final Date startDate,
			final Date endDate) {
		final List<ClinicStatistics.MonthlyTrend> trends = new ArrayList<>();

		// Determine date range
		LocalDate rangeStart;
		LocalDate rangeEnd;

		if (startDate == null || endDate == null) {
			// Use last 12 months if no range specified
			rangeEnd = LocalDate.now();
			rangeStart = rangeEnd.minusMonths(12);
		}
		else {
			rangeStart = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			rangeEnd = endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		}

		// Generate monthly data points
		final DateTimeFormatter displayFormat = DateTimeFormatter.ofPattern("MMM yyyy");
		YearMonth currentMonth = YearMonth.from(rangeStart);

		while (!currentMonth.atEndOfMonth().isAfter(rangeEnd)) {
			final LocalDate monthStart = currentMonth.atDay(1);
			final LocalDate monthEnd = currentMonth.atEndOfMonth();
			final Date monthStartDate = Date.from(monthStart.atStartOfDay(ZoneId.systemDefault()).toInstant());
			final Date monthEndDate = Date.from(monthEnd.atStartOfDay(ZoneId.systemDefault()).toInstant());

			final long consultations = calendarEventRepository.countByUserIdAndDateRange(userId, monthStartDate,
					monthEndDate);
			final long newPatients = pacienteRepository.findByUserId(userId)
				.stream()
				.filter(p -> {
					if (p.getRegistro() == null) {
						return false;
					}
					final LocalDate registro = p.getRegistro().toInstant().atZone(ZoneId.systemDefault())
						.toLocalDate();
					return !registro.isBefore(monthStart) && !registro.isAfter(monthEnd);
				})
				.count();
			final long activePlans = pacienteDietaRepository.findByUserIdAndDateRange(userId, monthStartDate,
					monthEndDate)
				.stream()
				.filter(pd -> pd.getStatus() == PacienteDietaStatus.ACTIVE)
				.count();

			final ClinicStatistics.MonthlyTrend trend = new ClinicStatistics.MonthlyTrend();
			trend.setMonth(monthStart.format(displayFormat));
			trend.setConsultations(consultations);
			trend.setNewPatients(newPatients);
			trend.setActivePlans(activePlans);

			trends.add(trend);

			currentMonth = currentMonth.plusMonths(1);
		}

		return trends;
	}

}
