package com.nutriconsultas.admin;

import java.util.ArrayList;
import java.util.Calendar;
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
		final Calendar calendar = Calendar.getInstance();
		calendar.setTime(now);
		calendar.add(Calendar.DAY_OF_MONTH, 7);
		final Date sevenDaysFromNow = calendar.getTime();

		final List<CalendarEvent> upcomingEvents = calendarEventRepository
			.findByUserIdAndDateRange(userId, now, sevenDaysFromNow)
			.stream()
			.filter(e -> e.getStatus() == EventStatus.SCHEDULED)
			.sorted(Comparator.comparing(CalendarEvent::getEventDateTime))
			.collect(Collectors.toList());

		stats.setUpcomingAppointments((long) upcomingEvents.size());
		stats.setUpcomingAppointmentsList(upcomingEvents.stream().limit(5).collect(Collectors.toList()));

		// Consultations this week
		calendar.setTime(now);
		calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		final Date startOfWeek = calendar.getTime();

		calendar.add(Calendar.DAY_OF_WEEK, 6);
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		final Date endOfWeek = calendar.getTime();

		final long consultationsThisWeek = calendarEventRepository.countByUserIdAndDateRange(userId, startOfWeek,
				endOfWeek);
		stats.setConsultationsThisWeek(consultationsThisWeek);

		// New patients this month
		calendar.setTime(now);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		final Date startOfMonth = calendar.getTime();

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
		calendar.setTime(now);
		calendar.add(Calendar.DAY_OF_MONTH, -30);
		final Date thirtyDaysAgo = calendar.getTime();

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
		final Calendar calendar = Calendar.getInstance();
		final Date now = new Date();
		calendar.setTime(now);

		for (int i = months - 1; i >= 0; i--) {
			calendar.setTime(now);
			calendar.add(Calendar.MONTH, -i);
			calendar.set(Calendar.DAY_OF_MONTH, 1);
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);
			final Date monthStart = calendar.getTime();

			calendar.add(Calendar.MONTH, 1);
			calendar.add(Calendar.MILLISECOND, -1);
			final Date monthEnd = calendar.getTime();

			final List<Paciente> patients = pacienteRepository.findByUserId(userId);
			final long count = patients.stream()
				.filter(p -> p.getRegistro() != null && !p.getRegistro().before(monthStart)
						&& !p.getRegistro().after(monthEnd))
				.count();

			final Map<String, Object> dataPoint = new HashMap<>();
			dataPoint.put("month",
					String.format("%02d/%d", calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR)));
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
		final Calendar calendar = Calendar.getInstance();
		final Date now = new Date();
		calendar.setTime(now);

		for (int i = months - 1; i >= 0; i--) {
			calendar.setTime(now);
			calendar.add(Calendar.MONTH, -i);
			calendar.set(Calendar.DAY_OF_MONTH, 1);
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);
			final Date monthStart = calendar.getTime();

			calendar.add(Calendar.MONTH, 1);
			calendar.add(Calendar.MILLISECOND, -1);
			final Date monthEnd = calendar.getTime();

			final long count = calendarEventRepository.countByUserIdAndDateRange(userId, monthStart, monthEnd);

			final Map<String, Object> dataPoint = new HashMap<>();
			dataPoint.put("month",
					String.format("%02d/%d", calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR)));
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
