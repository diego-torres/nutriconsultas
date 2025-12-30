// FILEPATH: /Users/dtorresf/Documents/GitHub/nutriconsultas/src/test/java/com/nutriconsultas/admin/DashboardControllerTest.java

package com.nutriconsultas.admin;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.calendar.CalendarEventService;
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
	private CalendarEventService calendarEventService;

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

		CalendarEvent event1 = new CalendarEvent();
		event1.setId(1L);
		event1.setTitle("Consulta de nutrición");
		event1.setEventDateTime(new Date(System.currentTimeMillis() + 86400000)); // Tomorrow
		event1.setDurationMinutes(60);
		event1.setStatus(EventStatus.SCHEDULED);
		event1.setPaciente(paciente);
		upcomingEvents.add(event1);

		CalendarEvent event2 = new CalendarEvent();
		event2.setId(2L);
		event2.setTitle("Seguimiento");
		event2.setEventDateTime(new Date(System.currentTimeMillis() + 172800000)); // Day
																					// after
																					// tomorrow
		event2.setDurationMinutes(30);
		event2.setStatus(EventStatus.SCHEDULED);
		event2.setPaciente(paciente);
		upcomingEvents.add(event2);

		when(calendarEventService.findUpcomingEvents(org.mockito.ArgumentMatchers.any(Date.class)))
			.thenReturn(upcomingEvents);
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testDashboardIndex() throws Exception {
		log.info("Starting testDashboardIndex");
		mockMvc.perform(MockMvcRequestBuilders.get("/admin"))
			.andExpect(MockMvcResultMatchers.status().isOk())
			.andExpect(MockMvcResultMatchers.view().name("sbadmin/index"))
			.andExpect(MockMvcResultMatchers.model().attribute("activeMenu", "home"))
			.andExpect(MockMvcResultMatchers.model().attributeExists("upcomingAppointments"));
		log.info("Finishing testDashboardIndex");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testDashboardWithUpcomingAppointments() throws Exception {
		log.info("Starting testDashboardWithUpcomingAppointments");
		mockMvc.perform(MockMvcRequestBuilders.get("/admin"))
			.andExpect(MockMvcResultMatchers.status().isOk())
			.andExpect(MockMvcResultMatchers.model().attribute("upcomingAppointments", upcomingEvents));
		log.info("Finishing testDashboardWithUpcomingAppointments");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testDashboardWithNoAppointments() throws Exception {
		log.info("Starting testDashboardWithNoAppointments");
		when(calendarEventService.findUpcomingEvents(org.mockito.ArgumentMatchers.any(Date.class)))
			.thenReturn(new ArrayList<>());

		mockMvc.perform(MockMvcRequestBuilders.get("/admin"))
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