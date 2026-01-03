package com.nutriconsultas.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.calendar.CalendarEventRepository;
import com.nutriconsultas.calendar.EventStatus;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteDietaRepository;
import com.nutriconsultas.paciente.PacienteDietaStatus;
import com.nutriconsultas.paciente.PacienteRepository;

import lombok.extern.slf4j.Slf4j;

@ExtendWith(MockitoExtension.class)
@Slf4j
@SuppressWarnings("null")
public class DashboardServiceTest {

	@InjectMocks
	private DashboardServiceImpl dashboardService;

	@Mock
	private PacienteRepository pacienteRepository;

	@Mock
	private CalendarEventRepository calendarEventRepository;

	@Mock
	private PacienteDietaRepository pacienteDietaRepository;

	private String userId;

	private List<Paciente> pacientes;

	private List<CalendarEvent> calendarEvents;

	@BeforeEach
	public void setup() {
		userId = "test-user-id";

		// Create test pacientes
		pacientes = new ArrayList<>();
		final Paciente paciente1 = new Paciente();
		paciente1.setId(1L);
		paciente1.setName("Juan Perez");
		paciente1.setUserId(userId);
		paciente1.setRegistro(new Date());
		paciente1.setHipertension(true);
		pacientes.add(paciente1);

		final Paciente paciente2 = new Paciente();
		paciente2.setId(2L);
		paciente2.setName("Maria Garcia");
		paciente2.setUserId(userId);
		final Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_MONTH, -10);
		paciente2.setRegistro(cal.getTime());
		paciente2.setDiabetes(true);
		pacientes.add(paciente2);

		// Create test calendar events
		calendarEvents = new ArrayList<>();
		final CalendarEvent event1 = new CalendarEvent();
		event1.setId(1L);
		event1.setTitle("Consulta de nutrición");
		final Calendar eventCal = Calendar.getInstance();
		eventCal.add(Calendar.DAY_OF_MONTH, 1);
		event1.setEventDateTime(eventCal.getTime());
		event1.setStatus(EventStatus.SCHEDULED);
		event1.setPaciente(paciente1);
		calendarEvents.add(event1);
	}

	private void setupMocksForGetDashboardStatistics() {
		when(pacienteRepository.findByUserId(userId)).thenReturn(pacientes);
		when(pacienteRepository.countByUserId(userId)).thenReturn((long) pacientes.size());
		when(calendarEventRepository.findByUserIdAndDateRange(anyString(), any(Date.class), any(Date.class)))
			.thenReturn(calendarEvents);
		when(calendarEventRepository.countByUserIdAndDateRange(anyString(), any(Date.class), any(Date.class)))
			.thenReturn((long) calendarEvents.size());
		when(pacienteDietaRepository.countByUserIdAndStatus(userId, PacienteDietaStatus.ACTIVE)).thenReturn(2L);
	}

	@Test
	public void testGetDashboardStatistics() {
		log.info("Testing getDashboardStatistics");
		setupMocksForGetDashboardStatistics();

		final DashboardStatistics stats = dashboardService.getDashboardStatistics(userId);

		assertThat(stats).isNotNull();
		assertThat(stats.getTotalPatients()).isEqualTo(2L);
		assertThat(stats.getActiveDietaryPlans()).isEqualTo(2L);
		assertThat(stats.getUpcomingAppointments()).isGreaterThanOrEqualTo(0L);
		assertThat(stats.getUpcomingAppointmentsList()).isNotNull();
		assertThat(stats.getRecentPatients()).isNotNull();
		assertThat(stats.getPatientsNeedingFollowUp()).isNotNull();
	}

	@Test
	public void testGetPatientGrowthTrend() {
		log.info("Testing getPatientGrowthTrend");
		when(pacienteRepository.findByUserId(userId)).thenReturn(pacientes);

		final List<Map<String, Object>> trend = dashboardService.getPatientGrowthTrend(userId, 6);

		assertThat(trend).isNotNull();
		assertThat(trend.size()).isEqualTo(6);
		for (final Map<String, Object> dataPoint : trend) {
			assertThat(dataPoint).containsKeys("month", "count");
			assertThat(dataPoint.get("count")).isInstanceOf(Long.class);
		}
	}

	@Test
	public void testGetConsultationFrequency() {
		log.info("Testing getConsultationFrequency");
		when(calendarEventRepository.countByUserIdAndDateRange(anyString(), any(Date.class), any(Date.class)))
			.thenReturn(0L);

		final List<Map<String, Object>> frequency = dashboardService.getConsultationFrequency(userId, 6);

		assertThat(frequency).isNotNull();
		assertThat(frequency.size()).isEqualTo(6);
		for (final Map<String, Object> dataPoint : frequency) {
			assertThat(dataPoint).containsKeys("month", "count");
			assertThat(dataPoint.get("count")).isInstanceOf(Long.class);
		}
	}

	@Test
	public void testGetMostCommonConditions() {
		log.info("Testing getMostCommonConditions");
		when(pacienteRepository.findByUserId(userId)).thenReturn(pacientes);

		final List<Map<String, Object>> conditions = dashboardService.getMostCommonConditions(userId);

		assertThat(conditions).isNotNull();
		if (!conditions.isEmpty()) {
			for (final Map<String, Object> condition : conditions) {
				assertThat(condition).containsKeys("condition", "count");
				assertThat(condition.get("count")).isInstanceOf(Long.class);
			}
		}
	}

	@Test
	public void testGetDashboardStatisticsWithNoData() {
		log.info("Testing getDashboardStatistics with no data");

		when(pacienteRepository.findByUserId(userId)).thenReturn(new ArrayList<>());
		when(pacienteRepository.countByUserId(userId)).thenReturn(0L);
		when(calendarEventRepository.findByUserIdAndDateRange(anyString(), any(Date.class), any(Date.class)))
			.thenReturn(new ArrayList<>());
		when(calendarEventRepository.countByUserIdAndDateRange(anyString(), any(Date.class), any(Date.class)))
			.thenReturn(0L);
		when(pacienteDietaRepository.countByUserIdAndStatus(userId, PacienteDietaStatus.ACTIVE)).thenReturn(0L);

		final DashboardStatistics stats = dashboardService.getDashboardStatistics(userId);

		assertThat(stats).isNotNull();
		assertThat(stats.getTotalPatients()).isEqualTo(0L);
		assertThat(stats.getActiveDietaryPlans()).isEqualTo(0L);
		assertThat(stats.getUpcomingAppointments()).isEqualTo(0L);
		assertThat(stats.getUpcomingAppointmentsList()).isEmpty();
		assertThat(stats.getRecentPatients()).isEmpty();
	}

}
