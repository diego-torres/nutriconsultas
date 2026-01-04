package com.nutriconsultas.admin;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.calendar.EventStatus;
import com.nutriconsultas.paciente.Paciente;

import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Slf4j
@SuppressWarnings("null")
public class DashboardControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private DashboardService dashboardService;

	private DashboardStatistics dashboardStatistics;

	private List<CalendarEvent> upcomingEvents;

	private Paciente paciente;

	@BeforeEach
	public void setup() {
		// Create test paciente
		paciente = new Paciente();
		paciente.setId(1L);
		paciente.setName("Juan Perez");

		// Create test upcoming events
		upcomingEvents = new ArrayList<>();

		final CalendarEvent event1 = new CalendarEvent();
		event1.setId(1L);
		event1.setTitle("Consulta de nutrición");
		event1.setEventDateTime(new Date(System.currentTimeMillis() + 86400000)); // Tomorrow
		event1.setDurationMinutes(60);
		event1.setStatus(EventStatus.SCHEDULED);
		event1.setPaciente(paciente);
		upcomingEvents.add(event1);

		final CalendarEvent event2 = new CalendarEvent();
		event2.setId(2L);
		event2.setTitle("Seguimiento");
		// Day after tomorrow
		event2.setEventDateTime(new Date(System.currentTimeMillis() + 172800000));
		event2.setDurationMinutes(30);
		event2.setStatus(EventStatus.SCHEDULED);
		event2.setPaciente(paciente);
		upcomingEvents.add(event2);

		// Create dashboard statistics
		dashboardStatistics = new DashboardStatistics();
		dashboardStatistics.setTotalPatients(10L);
		dashboardStatistics.setActiveDietaryPlans(5L);
		dashboardStatistics.setUpcomingAppointments(2L);
		dashboardStatistics.setConsultationsThisWeek(3L);
		dashboardStatistics.setNewPatientsThisMonth(2L);
		dashboardStatistics.setUpcomingAppointmentsList(upcomingEvents);
		dashboardStatistics.setRecentPatients(new ArrayList<>());
		dashboardStatistics.setPatientsNeedingFollowUp(new ArrayList<>());

		when(dashboardService.getDashboardStatistics(anyString())).thenReturn(dashboardStatistics);
		when(dashboardService.getPatientGrowthTrend(anyString(), org.mockito.ArgumentMatchers.anyInt()))
			.thenReturn(createMockTrendData());
		when(dashboardService.getConsultationFrequency(anyString(), org.mockito.ArgumentMatchers.anyInt()))
			.thenReturn(createMockTrendData());
		when(dashboardService.getMostCommonConditions(anyString())).thenReturn(createMockConditionsData());
	}

	private List<Map<String, Object>> createMockTrendData() {
		final List<Map<String, Object>> data = new ArrayList<>();
		for (int i = 0; i < 6; i++) {
			final Map<String, Object> point = new HashMap<>();
			point.put("month", String.format("0%d/2024", i + 1));
			point.put("count", (long) (i + 1));
			data.add(point);
		}
		return data;
	}

	private List<Map<String, Object>> createMockConditionsData() {
		final List<Map<String, Object>> data = new ArrayList<>();
		final Map<String, Object> condition1 = new HashMap<>();
		condition1.put("condition", "Hipertensión");
		condition1.put("count", 5L);
		data.add(condition1);
		final Map<String, Object> condition2 = new HashMap<>();
		condition2.put("condition", "Diabetes");
		condition2.put("count", 3L);
		data.add(condition2);
		return data;
	}

	private SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor oidcLogin() {
		return SecurityMockMvcRequestPostProcessors.oidcLogin()
			.idToken(token -> token.subject("test-user-id")
				.claim("name", "Test User")
				.claim("picture", "https://example.com/picture.jpg"));
	}

	@Test
	public void testDashboardIndex() throws Exception {
		log.info("Starting testDashboardIndex");
		mockMvc.perform(MockMvcRequestBuilders.get("/admin").with(oidcLogin()))
			.andExpect(MockMvcResultMatchers.status().isOk())
			.andExpect(MockMvcResultMatchers.view().name("sbadmin/index"))
			.andExpect(MockMvcResultMatchers.model().attribute("activeMenu", "home"))
			.andExpect(MockMvcResultMatchers.model().attributeExists("stats"))
			.andExpect(MockMvcResultMatchers.model().attributeExists("upcomingAppointments"))
			.andExpect(MockMvcResultMatchers.model().attributeExists("patientGrowthTrend"))
			.andExpect(MockMvcResultMatchers.model().attributeExists("consultationFrequency"))
			.andExpect(MockMvcResultMatchers.model().attributeExists("mostCommonConditions"));
		log.info("Finishing testDashboardIndex");
	}

	@Test
	public void testDashboardWithUpcomingAppointments() throws Exception {
		log.info("Starting testDashboardWithUpcomingAppointments");
		mockMvc.perform(MockMvcRequestBuilders.get("/admin").with(oidcLogin()))
			.andExpect(MockMvcResultMatchers.status().isOk())
			.andExpect(MockMvcResultMatchers.model().attribute("upcomingAppointments", upcomingEvents));
		log.info("Finishing testDashboardWithUpcomingAppointments");
	}

	@Test
	public void testDashboardWithNoAppointments() throws Exception {
		log.info("Starting testDashboardWithNoAppointments");
		final DashboardStatistics emptyStats = new DashboardStatistics();
		emptyStats.setUpcomingAppointmentsList(new ArrayList<>());
		when(dashboardService.getDashboardStatistics(anyString())).thenReturn(emptyStats);

		mockMvc.perform(MockMvcRequestBuilders.get("/admin").with(oidcLogin()))
			.andExpect(MockMvcResultMatchers.status().isOk())
			.andExpect(MockMvcResultMatchers.model().attribute("upcomingAppointments", org.hamcrest.Matchers.empty()));
		log.info("Finishing testDashboardWithNoAppointments");
	}

	@Test
	public void testPublicIndex() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/"))
			.andExpect(MockMvcResultMatchers.status().isOk())
			.andExpect(MockMvcResultMatchers.view().name("eterna/index"));
	}

}