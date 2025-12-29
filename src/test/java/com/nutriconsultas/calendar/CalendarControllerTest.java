package com.nutriconsultas.calendar;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;

import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@AutoConfigureMockMvc
@Slf4j
@ActiveProfiles("test")
@SuppressWarnings("null")
public class CalendarControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CalendarEventService calendarEventService;

	@MockitoBean
	private PacienteRepository pacienteRepository;

	private CalendarEvent event;

	private Paciente paciente;

	@BeforeEach
	public void setup() {
		// Create test paciente
		paciente = new Paciente();
		paciente.setId(1L);
		paciente.setName("Juan Perez");
		paciente.setEmail("juan@example.com");

		// Create test event
		event = new CalendarEvent();
		event.setId(1L);
		event.setTitle("Consulta de nutrición");
		event.setDescription("Primera consulta");
		event.setEventDateTime(new Date());
		event.setDurationMinutes(60);
		event.setStatus(EventStatus.SCHEDULED);
		event.setPaciente(paciente);

		// Setup mocks
		when(calendarEventService.findById(1L)).thenReturn(event);
		when(calendarEventService.findById(999L)).thenReturn(null);

		List<Paciente> pacientes = new ArrayList<>();
		pacientes.add(paciente);
		when(pacienteRepository.findAll()).thenReturn(pacientes);
		when(pacienteRepository.findById(1L)).thenReturn(java.util.Optional.of(paciente));
		when(pacienteRepository.findById(999L)).thenReturn(java.util.Optional.empty());
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testListado() throws Exception {
		log.info("Starting testListado");
		mockMvc.perform(MockMvcRequestBuilders.get("/admin/calendario"))
			.andExpect(status().isOk())
			.andExpect(MockMvcResultMatchers.view().name("sbadmin/calendar/listado"))
			.andExpect(MockMvcResultMatchers.model().attribute("activeMenu", "calendario"));
		log.info("Finishing testListado");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testNuevoEvento() throws Exception {
		log.info("Starting testNuevoEvento");
		mockMvc.perform(MockMvcRequestBuilders.get("/admin/calendario/nuevo"))
			.andExpect(status().isOk())
			.andExpect(MockMvcResultMatchers.view().name("sbadmin/calendar/formulario"))
			.andExpect(MockMvcResultMatchers.model().attribute("activeMenu", "calendario"))
			.andExpect(MockMvcResultMatchers.model().attributeExists("event"))
			.andExpect(MockMvcResultMatchers.model().attributeExists("pacientes"))
			.andExpect(MockMvcResultMatchers.model().attributeExists("statuses"));
		log.info("Finishing testNuevoEvento");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testVerEvento() throws Exception {
		log.info("Starting testVerEvento");
		mockMvc.perform(MockMvcRequestBuilders.get("/admin/calendario/1"))
			.andExpect(status().isOk())
			.andExpect(MockMvcResultMatchers.view().name("sbadmin/calendar/ver"))
			.andExpect(MockMvcResultMatchers.model().attribute("activeMenu", "calendario"))
			.andExpect(MockMvcResultMatchers.model().attribute("event", event));
		log.info("Finishing testVerEvento");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testVerEventoNotFound() throws Exception {
		log.info("Starting testVerEventoNotFound");
		mockMvc.perform(MockMvcRequestBuilders.get("/admin/calendario/999")).andExpect(status().is4xxClientError());
		log.info("Finishing testVerEventoNotFound");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testEditarEvento() throws Exception {
		log.info("Starting testEditarEvento");
		mockMvc.perform(MockMvcRequestBuilders.get("/admin/calendario/1/editar"))
			.andExpect(status().isOk())
			.andExpect(MockMvcResultMatchers.view().name("sbadmin/calendar/formulario"))
			.andExpect(MockMvcResultMatchers.model().attribute("activeMenu", "calendario"))
			.andExpect(MockMvcResultMatchers.model().attribute("event", event))
			.andExpect(MockMvcResultMatchers.model().attributeExists("pacientes"))
			.andExpect(MockMvcResultMatchers.model().attributeExists("statuses"));
		log.info("Finishing testEditarEvento");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testAgregarNuevoEvento() throws Exception {
		log.info("Starting testAgregarNuevoEvento");

		when(calendarEventService.save(any(CalendarEvent.class))).thenReturn(event);

		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/calendario/nuevo")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("pacienteId", "1")
				.param("title", "Nueva Consulta")
				.param("description", "Descripción de prueba")
				.param("eventDateTime", "2024-12-31T10:00")
				.param("durationMinutes", "60")
				.param("status", "SCHEDULED")
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/calendario"));

		verify(calendarEventService, times(1)).save(any(CalendarEvent.class));
		log.info("Finishing testAgregarNuevoEvento");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testAgregarNuevoEventoWithInvalidData() throws Exception {
		log.info("Starting testAgregarNuevoEventoWithInvalidData");

		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/calendario/nuevo")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("pacienteId", "1")
				.param("title", "") // Empty title should fail validation
				.param("eventDateTime", "2024-12-31T10:00")
				.param("durationMinutes", "60")
				.param("status", "SCHEDULED")
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().isOk())
			.andExpect(MockMvcResultMatchers.view().name("sbadmin/calendar/formulario"));

		verify(calendarEventService, never()).save(any(CalendarEvent.class));
		log.info("Finishing testAgregarNuevoEventoWithInvalidData");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testActualizarEvento() throws Exception {
		log.info("Starting testActualizarEvento");

		when(calendarEventService.save(any(CalendarEvent.class))).thenReturn(event);

		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/calendario/1/editar")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("pacienteId", "1")
				.param("title", "Consulta Actualizada")
				.param("description", "Descripción actualizada")
				.param("eventDateTime", "2024-12-31T14:00")
				.param("durationMinutes", "90")
				.param("status", "COMPLETED")
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/calendario"));

		verify(calendarEventService, times(1)).findById(1L);
		verify(calendarEventService, times(1)).save(any(CalendarEvent.class));
		log.info("Finishing testActualizarEvento");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testEliminarEvento() throws Exception {
		log.info("Starting testEliminarEvento");

		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/calendario/1/eliminar")
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/calendario"));

		verify(calendarEventService, times(1)).delete(1L);
		log.info("Finishing testEliminarEvento");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testAgregarEventoWithInvalidPaciente() throws Exception {
		log.info("Starting testAgregarEventoWithInvalidPaciente");

		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/calendario/nuevo")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("pacienteId", "999") // Non-existent paciente
				.param("title", "Nueva Consulta")
				.param("eventDateTime", "2024-12-31T10:00")
				.param("durationMinutes", "60")
				.param("status", "SCHEDULED")
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is4xxClientError());

		verify(calendarEventService, never()).save(any(CalendarEvent.class));
		log.info("Finishing testAgregarEventoWithInvalidPaciente");
	}

}
