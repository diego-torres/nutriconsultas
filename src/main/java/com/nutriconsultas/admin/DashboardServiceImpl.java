package com.nutriconsultas.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteDietaRepository;
import com.nutriconsultas.paciente.PacienteDietaStatus;
import com.nutriconsultas.paciente.PacienteRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DashboardServiceImpl implements DashboardService {

	@Autowired
	private PacienteRepository pacienteRepository;

	@Autowired
	private CalendarEventRepository calendarEventRepository;

	@Autowired
	private PacienteDietaRepository pacienteDietaRepository;

	@Override
	@Transactional(readOnly = true)
	public DashboardStatistics getDashboardStatistics(@NonNull final String userId) {
		log.info("Getting dashboard statistics for userId: {}", userId);

		final DashboardStatistics stats = new DashboardStatistics();

		// Total patients
		final long totalPatients = pacienteRepository.countByUserId(userId);
		stats.setTotalPatients(totalPatients);

		// Active dietary plans
		final long activeDietaryPlans = pacienteDietaRepository.countByUserIdAndStatus(userId,
				PacienteDietaStatus.ACTIVE);
		stats.setActiveDietaryPlans(activeDietaryPlans);

		// Upcoming appointments (next 7 days)
		final Date now = new Date();
		final LocalDateTime nowLocal = LocalDateTime.ofInstant(now.toInstant(), ZoneId.systemDefault());
		final LocalDateTime sevenDaysFromNowLocal = nowLocal.plusDays(7);
		final Date sevenDaysFromNow = Date.from(sevenDaysFromNowLocal.atZone(ZoneId.systemDefault()).toInstant());

		final List<CalendarEvent> upcomingEvents = calendarEventRepository
			.findByUserIdAndDateRange(userId, now, sevenDaysFromNow)
			.stream()
			.filter(e -> e.getStatus() == EventStatus.SCHEDULED)
			.sorted(Comparator.comparing(CalendarEvent::getEventDateTime))
			.collect(Collectors.toList());

		stats.setUpcomingAppointments((long) upcomingEvents.size());
		stats.setUpcomingAppointmentsList(upcomingEvents.stream().limit(5).collect(Collectors.toList()));

		// Consultations this week
		final LocalDate today = LocalDate.now();
		final LocalDate startOfWeekLocal = today.with(java.time.DayOfWeek.MONDAY);
		final LocalDate endOfWeekLocal = startOfWeekLocal.plusDays(6);
		final Date startOfWeek = Date.from(startOfWeekLocal.atStartOfDay(ZoneId.systemDefault()).toInstant());
		final Date endOfWeek = Date.from(endOfWeekLocal.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant());

		final long consultationsThisWeek = calendarEventRepository.countByUserIdAndDateRange(userId, startOfWeek,
				endOfWeek);
		stats.setConsultationsThisWeek(consultationsThisWeek);

		// New patients this month
		final LocalDate startOfMonthLocal = LocalDate.now().withDayOfMonth(1);
		final Date startOfMonth = Date.from(startOfMonthLocal.atStartOfDay(ZoneId.systemDefault()).toInstant());

		final List<Paciente> allPatients = pacienteRepository.findByUserId(userId);
		final long newPatientsThisMonth = allPatients.stream()
			.filter(p -> p.getRegistro() != null && !p.getRegistro().before(startOfMonth))
			.count();
		stats.setNewPatientsThisMonth(newPatientsThisMonth);

		// Recent patients (last 5, ordered by registration date)
		final List<Paciente> recentPatients = allPatients.stream()
			.filter(p -> p.getRegistro() != null)
			.sorted(Comparator.comparing(Paciente::getRegistro).reversed())
			.limit(5)
			.collect(Collectors.toList());
		stats.setRecentPatients(recentPatients);

		// Patients needing follow-up (patients with no appointments in the last 30 days)
		final LocalDateTime thirtyDaysAgoLocal = nowLocal.minusDays(30);
		final Date thirtyDaysAgo = Date.from(thirtyDaysAgoLocal.atZone(ZoneId.systemDefault()).toInstant());

		final List<Paciente> patientsNeedingFollowUp = new ArrayList<>();
		for (final Paciente paciente : allPatients) {
			final List<CalendarEvent> recentEvents = calendarEventRepository
				.findByUserIdAndDateRange(userId, thirtyDaysAgo, now)
				.stream()
				.filter(e -> e.getPaciente() != null && e.getPaciente().getId().equals(paciente.getId()))
				.collect(Collectors.toList());
			if (recentEvents.isEmpty()) {
				patientsNeedingFollowUp.add(paciente);
			}
		}
		stats.setPatientsNeedingFollowUp(patientsNeedingFollowUp.stream().limit(5).collect(Collectors.toList()));

		log.info("Dashboard statistics retrieved: totalPatients={}, activeDietaryPlans={}, upcomingAppointments={}",
				totalPatients, activeDietaryPlans, upcomingEvents.size());

		return stats;
	}

	@Override
	@Transactional(readOnly = true)
	public List<Map<String, Object>> getPatientGrowthTrend(@NonNull final String userId, final int months) {
		log.info("Getting patient growth trend for userId: {} for {} months", userId, months);

		final List<Map<String, Object>> trend = new ArrayList<>();
		final LocalDate now = LocalDate.now();

		for (int i = months - 1; i >= 0; i--) {
			final LocalDate monthStartLocal = now.minusMonths(i).withDayOfMonth(1);
			final LocalDate monthEndLocal = monthStartLocal.plusMonths(1).minusDays(1);
			final Date monthStart = Date.from(monthStartLocal.atStartOfDay(ZoneId.systemDefault()).toInstant());
			final Date monthEnd = Date
				.from(monthEndLocal.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant());

			final List<Paciente> patients = pacienteRepository.findByUserId(userId);
			final long count = patients.stream()
				.filter(p -> p.getRegistro() != null && !p.getRegistro().before(monthStart)
						&& !p.getRegistro().after(monthEnd))
				.count();

			final Map<String, Object> dataPoint = new HashMap<>();
			dataPoint.put("month",
					String.format("%02d/%d", monthStartLocal.getMonthValue(), monthStartLocal.getYear()));
			dataPoint.put("count", count);
			trend.add(dataPoint);
		}

		return trend;
	}

	@Override
	@Transactional(readOnly = true)
	public List<Map<String, Object>> getConsultationFrequency(@NonNull final String userId, final int months) {
		log.info("Getting consultation frequency for userId: {} for {} months", userId, months);

		final List<Map<String, Object>> frequency = new ArrayList<>();
		final LocalDate now = LocalDate.now();

		for (int i = months - 1; i >= 0; i--) {
			final LocalDate monthStartLocal = now.minusMonths(i).withDayOfMonth(1);
			final LocalDate monthEndLocal = monthStartLocal.plusMonths(1).minusDays(1);
			final Date monthStart = Date.from(monthStartLocal.atStartOfDay(ZoneId.systemDefault()).toInstant());
			final Date monthEnd = Date
				.from(monthEndLocal.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant());

			final long count = calendarEventRepository.countByUserIdAndDateRange(userId, monthStart, monthEnd);

			final Map<String, Object> dataPoint = new HashMap<>();
			dataPoint.put("month",
					String.format("%02d/%d", monthStartLocal.getMonthValue(), monthStartLocal.getYear()));
			dataPoint.put("count", count);
			frequency.add(dataPoint);
		}

		return frequency;
	}

	@Override
	@Transactional(readOnly = true)
	public List<Map<String, Object>> getMostCommonConditions(@NonNull final String userId) {
		log.info("Getting most common conditions for userId: {}", userId);

		final List<Paciente> patients = pacienteRepository.findByUserId(userId);

		final Map<String, Long> conditions = new HashMap<>();
		conditions.put("Hipertensión", patients.stream().filter(p -> Boolean.TRUE.equals(p.getHipertension())).count());
		conditions.put("Diabetes", patients.stream().filter(p -> Boolean.TRUE.equals(p.getDiabetes())).count());
		conditions.put("Hipotiroidismo",
				patients.stream().filter(p -> Boolean.TRUE.equals(p.getHipotiroidismo())).count());
		conditions.put("Obesidad", patients.stream().filter(p -> Boolean.TRUE.equals(p.getObesidad())).count());
		conditions.put("Anemia", patients.stream().filter(p -> Boolean.TRUE.equals(p.getAnemia())).count());
		conditions.put("Bulimia", patients.stream().filter(p -> Boolean.TRUE.equals(p.getBulimia())).count());
		conditions.put("Anorexia", patients.stream().filter(p -> Boolean.TRUE.equals(p.getAnorexia())).count());
		conditions.put("Enfermedades Hepáticas",
				patients.stream().filter(p -> Boolean.TRUE.equals(p.getEnfermedadesHepaticas())).count());

		// Sort by count descending and limit to top 5
		final List<Map<String, Object>> result = conditions.entrySet()
			.stream()
			.filter(e -> e.getValue() > 0)
			.sorted(Map.Entry.<String, Long>comparingByValue().reversed())
			.limit(5)
			.map(e -> {
				final Map<String, Object> item = new LinkedHashMap<>();
				item.put("condition", e.getKey());
				item.put("count", e.getValue());
				return item;
			})
			.collect(Collectors.toList());

		return result.isEmpty() ? Collections.emptyList() : result;
	}

}
