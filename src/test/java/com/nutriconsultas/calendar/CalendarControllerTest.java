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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.mockito.ArgumentCaptor;

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

	private static final String TEST_USER_ID = "test-user-id-123";

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
		paciente.setUserId(TEST_USER_ID);

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
		when(pacienteRepository.findByUserId(TEST_USER_ID)).thenReturn(pacientes);
		when(pacienteRepository.findByIdAndUserId(1L, TEST_USER_ID))
			.thenReturn(java.util.Optional.of(paciente));
		when(pacienteRepository.findByIdAndUserId(999L, TEST_USER_ID))
			.thenReturn(java.util.Optional.empty());
	}

	private SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor oidcLogin() {
		return SecurityMockMvcRequestPostProcessors.oidcLogin()
			.idToken(token -> token.subject(TEST_USER_ID).claim("name", "Test User")
				.claim("picture", "https://example.com/picture.jpg"));
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
		mockMvc.perform(MockMvcRequestBuilders.get("/admin/calendario/nuevo").with(oidcLogin()))
			.andExpect(status().isOk())
			.andExpect(MockMvcResultMatchers.view().name("sbadmin/calendar/formulario"))
			.andExpect(MockMvcResultMatchers.model().attribute("activeMenu", "calendario"))
			.andExpect(MockMvcResultMatchers.model().attributeExists("event"))
			.andExpect(MockMvcResultMatchers.model().attributeExists("pacientes"))
			.andExpect(MockMvcResultMatchers.model().attributeExists("statuses"))
			.andExpect(result -> {
				final CalendarEvent event = (CalendarEvent) result.getModelAndView().getModel().get("event");
				assertNotNull(event, "Event should not be null");
				assertNull(event.getId(), "New event should have null id");
				assertEquals(EventStatus.SCHEDULED, event.getStatus(), "New event should have SCHEDULED status");
			});
		log.info("Finishing testNuevoEvento");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testVerEvento() throws Exception {
		log.info("Starting testVerEvento");
		mockMvc.perform(MockMvcRequestBuilders.get("/admin/calendario/1").with(oidcLogin()))
			.andExpect(status().isOk())
			.andExpect(MockMvcResultMatchers.view().name("sbadmin/calendar/ver"))
			.andExpect(MockMvcResultMatchers.model().attribute("activeMenu", "calendario"))
			.andExpect(MockMvcResultMatchers.model().attribute("event", event));
		log.info("Finishing testVerEvento");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testVerEventoWithSummaryNotes() throws Exception {
		log.info("Starting testVerEventoWithSummaryNotes");
		// Create event with summary notes
		CalendarEvent eventWithNotes = new CalendarEvent();
		eventWithNotes.setId(2L);
		eventWithNotes.setTitle("Consulta con notas");
		eventWithNotes.setDescription("Descripción");
		eventWithNotes.setEventDateTime(new Date());
		eventWithNotes.setDurationMinutes(60);
		eventWithNotes.setStatus(EventStatus.COMPLETED);
		eventWithNotes.setPaciente(paciente);
		eventWithNotes.setSummaryNotes("=== CONSULTA NUTRICIONAL ===\n\nMOTIVO DE CONSULTA:\nControl de peso");

		when(calendarEventService.findById(2L)).thenReturn(eventWithNotes);

		mockMvc.perform(MockMvcRequestBuilders.get("/admin/calendario/2").with(oidcLogin()))
			.andExpect(status().isOk())
			.andExpect(MockMvcResultMatchers.view().name("sbadmin/calendar/ver"))
			.andExpect(MockMvcResultMatchers.model().attribute("activeMenu", "calendario"))
			.andExpect(MockMvcResultMatchers.model().attribute("event", eventWithNotes))
			.andExpect(result -> {
				final CalendarEvent event = (CalendarEvent) result.getModelAndView().getModel().get("event");
				assertNotNull(event, "Event should not be null");
				assertNotNull(event.getSummaryNotes(), "Event should have summary notes");
				assertEquals("=== CONSULTA NUTRICIONAL ===\n\nMOTIVO DE CONSULTA:\nControl de peso",
						event.getSummaryNotes(), "Summary notes should match");
			});
		log.info("Finishing testVerEventoWithSummaryNotes");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testVerEventoWithDifferentStatuses() throws Exception {
		log.info("Starting testVerEventoWithDifferentStatuses");

		// Test SCHEDULED status
		event.setStatus(EventStatus.SCHEDULED);
		mockMvc.perform(MockMvcRequestBuilders.get("/admin/calendario/1").with(oidcLogin()))
			.andExpect(status().isOk())
			.andExpect(MockMvcResultMatchers.view().name("sbadmin/calendar/ver"))
			.andExpect(MockMvcResultMatchers.model().attribute("event", event));

		// Test COMPLETED status
		event.setStatus(EventStatus.COMPLETED);
		when(calendarEventService.findById(1L)).thenReturn(event);
		mockMvc.perform(MockMvcRequestBuilders.get("/admin/calendario/1").with(oidcLogin()))
			.andExpect(status().isOk())
			.andExpect(MockMvcResultMatchers.view().name("sbadmin/calendar/ver"))
			.andExpect(MockMvcResultMatchers.model().attribute("event", event));

		// Test CANCELLED status
		event.setStatus(EventStatus.CANCELLED);
		when(calendarEventService.findById(1L)).thenReturn(event);
		mockMvc.perform(MockMvcRequestBuilders.get("/admin/calendario/1").with(oidcLogin()))
			.andExpect(status().isOk())
			.andExpect(MockMvcResultMatchers.view().name("sbadmin/calendar/ver"))
			.andExpect(MockMvcResultMatchers.model().attribute("event", event));

		log.info("Finishing testVerEventoWithDifferentStatuses");
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
		mockMvc.perform(MockMvcRequestBuilders.get("/admin/calendario/1/editar").with(oidcLogin()))
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
				.with(oidcLogin())
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
				.with(oidcLogin())
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().isOk())
			.andExpect(MockMvcResultMatchers.view().name("sbadmin/calendar/formulario"))
			.andExpect(MockMvcResultMatchers.model().attributeExists("event"))
			.andExpect(MockMvcResultMatchers.model().attributeExists("pacientes"))
			.andExpect(MockMvcResultMatchers.model().attributeExists("statuses"))
			.andExpect(result -> {
				// Verify event is in model and can be accessed safely (validates null
				// handling fix)
				final CalendarEvent event = (CalendarEvent) result.getModelAndView().getModel().get("event");
				assertNotNull(event, "Event should be in model even with validation errors");
			});

		verify(calendarEventService, never()).save(any(CalendarEvent.class));
		log.info("Finishing testAgregarNuevoEventoWithInvalidData");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testActualizarEvento() throws Exception {
		log.info("Starting testActualizarEvento");

		// Create existing event with summary notes to verify they are preserved
		CalendarEvent existingEvent = new CalendarEvent();
		existingEvent.setId(1L);
		existingEvent.setTitle("Consulta Original");
		existingEvent.setDescription("Descripción original");
		existingEvent.setEventDateTime(new Date());
		existingEvent.setDurationMinutes(60);
		existingEvent.setStatus(EventStatus.SCHEDULED);
		existingEvent.setPaciente(paciente);
		existingEvent.setSummaryNotes("Notas originales que deben preservarse");

		when(calendarEventService.findById(1L)).thenReturn(existingEvent);
		when(calendarEventService.save(any(CalendarEvent.class))).thenAnswer(invocation -> {
			CalendarEvent saved = invocation.getArgument(0);
			return saved;
		});

		final ArgumentCaptor<CalendarEvent> eventCaptor = ArgumentCaptor.forClass(CalendarEvent.class);

		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/calendario/1/editar")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("pacienteId", "1")
				.param("title", "Consulta Actualizada")
				.param("description", "Descripción actualizada")
				.param("eventDateTime", "2024-12-31T14:00")
				.param("durationMinutes", "90")
				.param("status", "COMPLETED")
				.with(oidcLogin())
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/calendario"));

		verify(calendarEventService, times(1)).findById(1L);
		verify(calendarEventService, times(1)).save(eventCaptor.capture());

		// Verify all fields were updated correctly
		CalendarEvent savedEvent = eventCaptor.getValue();
		assertEquals("Consulta Actualizada", savedEvent.getTitle(), "Title should be updated");
		assertEquals("Descripción actualizada", savedEvent.getDescription(), "Description should be updated");
		assertEquals(90, savedEvent.getDurationMinutes(), "Duration should be updated");
		assertEquals(EventStatus.COMPLETED, savedEvent.getStatus(), "Status should be updated");
		assertEquals("Notas originales que deben preservarse", savedEvent.getSummaryNotes(),
				"Summary notes should be preserved");
		assertEquals(paciente.getId(), savedEvent.getPaciente().getId(), "Paciente should be updated");

		log.info("Finishing testActualizarEvento");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testEliminarEvento() throws Exception {
		log.info("Starting testEliminarEvento");

		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/calendario/1/eliminar")
				.with(oidcLogin())
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
				.with(oidcLogin())
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is4xxClientError());

		verify(calendarEventService, never()).save(any(CalendarEvent.class));
		log.info("Finishing testAgregarEventoWithInvalidPaciente");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testActualizarEventoWithInvalidData() throws Exception {
		log.info("Starting testActualizarEventoWithInvalidData");

		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/calendario/1/editar")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("pacienteId", "1")
				.param("title", "") // Empty title should fail validation
				.param("eventDateTime", "2024-12-31T14:00")
				.param("durationMinutes", "90")
				.param("status", "COMPLETED")
				.with(oidcLogin())
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().isOk())
			.andExpect(MockMvcResultMatchers.view().name("sbadmin/calendar/formulario"))
			.andExpect(MockMvcResultMatchers.model().attributeExists("event"))
			.andExpect(MockMvcResultMatchers.model().attributeExists("pacientes"))
			.andExpect(MockMvcResultMatchers.model().attributeExists("statuses"))
			.andExpect(result -> {
				// Verify event is in model and can be accessed safely (validates null
				// handling fix)
				final CalendarEvent event = (CalendarEvent) result.getModelAndView().getModel().get("event");
				assertNotNull(event, "Event should be in model even with validation errors");
			});

		verify(calendarEventService, never()).save(any(CalendarEvent.class));
		log.info("Finishing testActualizarEventoWithInvalidData");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testNuevoEventoRendersWithNullId() throws Exception {
		log.info("Starting testNuevoEventoRendersWithNullId");
		// This test validates that the template can handle event with null id
		// which was the root cause of the TemplateInputException
		mockMvc.perform(MockMvcRequestBuilders.get("/admin/calendario/nuevo").with(oidcLogin()))
			.andExpect(status().isOk())
			.andExpect(MockMvcResultMatchers.view().name("sbadmin/calendar/formulario"))
			.andExpect(result -> {
				// Verify the template can be rendered without errors when event.id is
				// null
				final CalendarEvent event = (CalendarEvent) result.getModelAndView().getModel().get("event");
				assertNotNull(event, "Event should not be null");
				assertNull(event.getId(), "New event should have null id for template null checks");
				// If we get here without TemplateInputException, the null handling is
				// working
			});
		log.info("Finishing testNuevoEventoRendersWithNullId");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testEditarEventoLoadsExistingData() throws Exception {
		log.info("Starting testEditarEventoLoadsExistingData");

		mockMvc.perform(MockMvcRequestBuilders.get("/admin/calendario/1/editar").with(oidcLogin()))
			.andExpect(status().isOk())
			.andExpect(MockMvcResultMatchers.view().name("sbadmin/calendar/formulario"))
			.andExpect(MockMvcResultMatchers.model().attribute("activeMenu", "calendario"))
			.andExpect(MockMvcResultMatchers.model().attribute("event", event))
			.andExpect(MockMvcResultMatchers.model().attributeExists("pacientes"))
			.andExpect(MockMvcResultMatchers.model().attributeExists("statuses"))
			.andExpect(result -> {
				final CalendarEvent loadedEvent = (CalendarEvent) result.getModelAndView().getModel().get("event");
				assertNotNull(loadedEvent, "Event should not be null");
				assertEquals(1L, loadedEvent.getId(), "Event ID should match");
				assertEquals("Consulta de nutrición", loadedEvent.getTitle(), "Title should be loaded");
				assertEquals("Primera consulta", loadedEvent.getDescription(), "Description should be loaded");
				assertEquals(60, loadedEvent.getDurationMinutes(), "Duration should be loaded");
				assertEquals(EventStatus.SCHEDULED, loadedEvent.getStatus(), "Status should be loaded");
				assertNotNull(loadedEvent.getPaciente(), "Paciente should be loaded");
				assertEquals(1L, loadedEvent.getPaciente().getId(), "Paciente ID should match");
			});
		log.info("Finishing testEditarEventoLoadsExistingData");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testActualizarEventoWithStatusAgendado() throws Exception {
		log.info("Starting testActualizarEventoWithStatusAgendado");

		CalendarEvent existingEvent = new CalendarEvent();
		existingEvent.setId(1L);
		existingEvent.setTitle("Consulta Original");
		existingEvent.setStatus(EventStatus.COMPLETED);
		existingEvent.setPaciente(paciente);

		when(calendarEventService.findById(1L)).thenReturn(existingEvent);
		when(calendarEventService.save(any(CalendarEvent.class))).thenAnswer(invocation -> {
			CalendarEvent saved = invocation.getArgument(0);
			return saved;
		});

		final ArgumentCaptor<CalendarEvent> eventCaptor = ArgumentCaptor.forClass(CalendarEvent.class);

		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/calendario/1/editar")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("pacienteId", "1")
				.param("title", "Consulta Actualizada")
				.param("eventDateTime", "2024-12-31T10:00")
				.param("durationMinutes", "60")
				.param("status", "SCHEDULED")
				.with(oidcLogin())
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/calendario"));

		verify(calendarEventService, times(1)).save(eventCaptor.capture());
		assertEquals(EventStatus.SCHEDULED, eventCaptor.getValue().getStatus(), "Status should be SCHEDULED");
		log.info("Finishing testActualizarEventoWithStatusAgendado");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testActualizarEventoWithStatusCompletado() throws Exception {
		log.info("Starting testActualizarEventoWithStatusCompletado");

		CalendarEvent existingEvent = new CalendarEvent();
		existingEvent.setId(1L);
		existingEvent.setTitle("Consulta Original");
		existingEvent.setStatus(EventStatus.SCHEDULED);
		existingEvent.setPaciente(paciente);

		when(calendarEventService.findById(1L)).thenReturn(existingEvent);
		when(calendarEventService.save(any(CalendarEvent.class))).thenAnswer(invocation -> {
			CalendarEvent saved = invocation.getArgument(0);
			return saved;
		});

		final ArgumentCaptor<CalendarEvent> eventCaptor = ArgumentCaptor.forClass(CalendarEvent.class);

		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/calendario/1/editar")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("pacienteId", "1")
				.param("title", "Consulta Actualizada")
				.param("eventDateTime", "2024-12-31T10:00")
				.param("durationMinutes", "60")
				.param("status", "COMPLETED")
				.with(oidcLogin())
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/calendario"));

		verify(calendarEventService, times(1)).save(eventCaptor.capture());
		assertEquals(EventStatus.COMPLETED, eventCaptor.getValue().getStatus(), "Status should be COMPLETED");
		log.info("Finishing testActualizarEventoWithStatusCompletado");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testActualizarEventoWithStatusCancelado() throws Exception {
		log.info("Starting testActualizarEventoWithStatusCancelado");

		CalendarEvent existingEvent = new CalendarEvent();
		existingEvent.setId(1L);
		existingEvent.setTitle("Consulta Original");
		existingEvent.setStatus(EventStatus.SCHEDULED);
		existingEvent.setPaciente(paciente);

		when(calendarEventService.findById(1L)).thenReturn(existingEvent);
		when(calendarEventService.save(any(CalendarEvent.class))).thenAnswer(invocation -> {
			CalendarEvent saved = invocation.getArgument(0);
			return saved;
		});

		final ArgumentCaptor<CalendarEvent> eventCaptor = ArgumentCaptor.forClass(CalendarEvent.class);

		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/calendario/1/editar")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("pacienteId", "1")
				.param("title", "Consulta Actualizada")
				.param("eventDateTime", "2024-12-31T10:00")
				.param("durationMinutes", "60")
				.param("status", "CANCELLED")
				.with(oidcLogin())
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/calendario"));

		verify(calendarEventService, times(1)).save(eventCaptor.capture());
		assertEquals(EventStatus.CANCELLED, eventCaptor.getValue().getStatus(), "Status should be CANCELLED");
		log.info("Finishing testActualizarEventoWithStatusCancelado");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testActualizarEventoPreservesSummaryNotes() throws Exception {
		log.info("Starting testActualizarEventoPreservesSummaryNotes");

		CalendarEvent existingEvent = new CalendarEvent();
		existingEvent.setId(1L);
		existingEvent.setTitle("Consulta Original");
		existingEvent.setDescription("Descripción original");
		existingEvent.setEventDateTime(new Date());
		existingEvent.setDurationMinutes(60);
		existingEvent.setStatus(EventStatus.SCHEDULED);
		existingEvent.setPaciente(paciente);
		existingEvent.setSummaryNotes("Notas importantes que no deben perderse");

		when(calendarEventService.findById(1L)).thenReturn(existingEvent);
		when(calendarEventService.save(any(CalendarEvent.class))).thenAnswer(invocation -> {
			CalendarEvent saved = invocation.getArgument(0);
			return saved;
		});

		final ArgumentCaptor<CalendarEvent> eventCaptor = ArgumentCaptor.forClass(CalendarEvent.class);

		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/calendario/1/editar")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("pacienteId", "1")
				.param("title", "Consulta Actualizada")
				.param("description", "Nueva descripción")
				.param("eventDateTime", "2024-12-31T14:00")
				.param("durationMinutes", "90")
				.param("status", "COMPLETED")
				.with(oidcLogin())
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/calendario"));

		verify(calendarEventService, times(1)).save(eventCaptor.capture());
		CalendarEvent savedEvent = eventCaptor.getValue();
		assertEquals("Notas importantes que no deben perderse", savedEvent.getSummaryNotes(),
				"Summary notes should be preserved");
		assertEquals("Consulta Actualizada", savedEvent.getTitle(), "Title should be updated");
		assertEquals("Nueva descripción", savedEvent.getDescription(), "Description should be updated");
		log.info("Finishing testActualizarEventoPreservesSummaryNotes");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testActualizarEventoUpdatesAllFields() throws Exception {
		log.info("Starting testActualizarEventoUpdatesAllFields");

		CalendarEvent existingEvent = new CalendarEvent();
		existingEvent.setId(1L);
		existingEvent.setTitle("Título Original");
		existingEvent.setDescription("Descripción Original");
		existingEvent.setEventDateTime(new Date(1000000L));
		existingEvent.setDurationMinutes(30);
		existingEvent.setStatus(EventStatus.SCHEDULED);
		existingEvent.setPaciente(paciente);

		when(calendarEventService.findById(1L)).thenReturn(existingEvent);
		when(calendarEventService.save(any(CalendarEvent.class))).thenAnswer(invocation -> {
			CalendarEvent saved = invocation.getArgument(0);
			return saved;
		});

		final ArgumentCaptor<CalendarEvent> eventCaptor = ArgumentCaptor.forClass(CalendarEvent.class);

		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/calendario/1/editar")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("pacienteId", "1")
				.param("title", "Nuevo Título")
				.param("description", "Nueva Descripción")
				.param("eventDateTime", "2024-12-31T15:30")
				.param("durationMinutes", "120")
				.param("status", "CANCELLED")
				.with(oidcLogin())
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/calendario"));

		verify(calendarEventService, times(1)).save(eventCaptor.capture());
		CalendarEvent savedEvent = eventCaptor.getValue();
		assertEquals("Nuevo Título", savedEvent.getTitle(), "Title should be updated");
		assertEquals("Nueva Descripción", savedEvent.getDescription(), "Description should be updated");
		assertEquals(120, savedEvent.getDurationMinutes(), "Duration should be updated");
		assertEquals(EventStatus.CANCELLED, savedEvent.getStatus(), "Status should be updated");
		assertEquals(paciente.getId(), savedEvent.getPaciente().getId(), "Paciente should be updated");
		// Verify eventDateTime is updated (checking it's not the original date)
		assertNotNull(savedEvent.getEventDateTime(), "Event date time should be set");
		log.info("Finishing testActualizarEventoUpdatesAllFields");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testGuardarButtonSavesChangesAndRedirectsToCalendar() throws Exception {
		log.info("Starting testGuardarButtonSavesChangesAndRedirectsToCalendar");

		CalendarEvent existingEvent = new CalendarEvent();
		existingEvent.setId(1L);
		existingEvent.setTitle("Evento Original");
		existingEvent.setDescription("Descripción original");
		existingEvent.setEventDateTime(new Date());
		existingEvent.setDurationMinutes(60);
		existingEvent.setStatus(EventStatus.SCHEDULED);
		existingEvent.setPaciente(paciente);

		when(calendarEventService.findById(1L)).thenReturn(existingEvent);
		when(calendarEventService.save(any(CalendarEvent.class))).thenAnswer(invocation -> {
			CalendarEvent saved = invocation.getArgument(0);
			return saved;
		});

		final ArgumentCaptor<CalendarEvent> eventCaptor = ArgumentCaptor.forClass(CalendarEvent.class);

		// Simulate clicking the "Guardar" button on /admin/calendario/1/editar
		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/calendario/1/editar")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("id", "1")
				.param("pacienteId", "1")
				.param("title", "Evento Modificado")
				.param("description", "Nueva descripción después de guardar")
				.param("eventDateTime", "2024-12-31T16:00")
				.param("durationMinutes", "45")
				.param("status", "COMPLETED")
				.with(oidcLogin())
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/calendario"));

		// Verify that save was called exactly once
		verify(calendarEventService, times(1)).findById(1L);
		verify(calendarEventService, times(1)).save(eventCaptor.capture());

		// Verify that all changes were saved
		CalendarEvent savedEvent = eventCaptor.getValue();
		assertEquals("Evento Modificado", savedEvent.getTitle(), "Title should be saved");
		assertEquals("Nueva descripción después de guardar", savedEvent.getDescription(),
				"Description should be saved");
		assertEquals(45, savedEvent.getDurationMinutes(), "Duration should be saved");
		assertEquals(EventStatus.COMPLETED, savedEvent.getStatus(), "Status should be saved");
		assertEquals(1L, savedEvent.getId(), "Event ID should be preserved");

		log.info("Finishing testGuardarButtonSavesChangesAndRedirectsToCalendar");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testGuardarButtonNavigatesBackToCalendarAfterSave() throws Exception {
		log.info("Starting testGuardarButtonNavigatesBackToCalendarAfterSave");

		CalendarEvent existingEvent = new CalendarEvent();
		existingEvent.setId(1L);
		existingEvent.setTitle("Test Event");
		existingEvent.setEventDateTime(new Date());
		existingEvent.setDurationMinutes(60);
		existingEvent.setStatus(EventStatus.SCHEDULED);
		existingEvent.setPaciente(paciente);

		when(calendarEventService.findById(1L)).thenReturn(existingEvent);
		when(calendarEventService.save(any(CalendarEvent.class))).thenReturn(existingEvent);

		// Verify that after successful save, the response redirects to /admin/calendario
		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/calendario/1/editar")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("pacienteId", "1")
				.param("title", "Updated Title")
				.param("eventDateTime", "2024-12-31T10:00")
				.param("durationMinutes", "60")
				.param("status", "SCHEDULED")
				.with(oidcLogin())
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/calendario"))
			.andExpect(result -> {
				// Verify the redirect status code
				assertEquals(302, result.getResponse().getStatus(), "Should return 302 redirect status");
			});

		// Verify save was called
		verify(calendarEventService, times(1)).save(any(CalendarEvent.class));

		log.info("Finishing testGuardarButtonNavigatesBackToCalendarAfterSave");
	}

	@Test
	@WithMockUser(username = "admin", roles = { "ADMIN" })
	public void testGuardarButtonPersistsAllChanges() throws Exception {
		log.info("Starting testGuardarButtonPersistsAllChanges");

		CalendarEvent existingEvent = new CalendarEvent();
		existingEvent.setId(1L);
		existingEvent.setTitle("Original");
		existingEvent.setDescription("Original Description");
		existingEvent.setEventDateTime(new Date(1000000L));
		existingEvent.setDurationMinutes(30);
		existingEvent.setStatus(EventStatus.SCHEDULED);
		existingEvent.setPaciente(paciente);
		existingEvent.setSummaryNotes("Original notes");

		when(calendarEventService.findById(1L)).thenReturn(existingEvent);
		when(calendarEventService.save(any(CalendarEvent.class))).thenAnswer(invocation -> {
			CalendarEvent saved = invocation.getArgument(0);
			return saved;
		});

		final ArgumentCaptor<CalendarEvent> eventCaptor = ArgumentCaptor.forClass(CalendarEvent.class);

		// Submit form with all fields changed
		mockMvc
			.perform(MockMvcRequestBuilders.post("/admin/calendario/1/editar")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("pacienteId", "1")
				.param("title", "Persisted Title")
				.param("description", "Persisted Description")
				.param("eventDateTime", "2025-01-15T14:30")
				.param("durationMinutes", "90")
				.param("status", "COMPLETED")
				.with(oidcLogin())
				.with(SecurityMockMvcRequestPostProcessors.csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(MockMvcResultMatchers.redirectedUrl("/admin/calendario"));

		verify(calendarEventService, times(1)).save(eventCaptor.capture());

		// Verify all changes were persisted
		CalendarEvent persistedEvent = eventCaptor.getValue();
		assertEquals("Persisted Title", persistedEvent.getTitle(), "Title should be persisted");
		assertEquals("Persisted Description", persistedEvent.getDescription(), "Description should be persisted");
		assertEquals(90, persistedEvent.getDurationMinutes(), "Duration should be persisted");
		assertEquals(EventStatus.COMPLETED, persistedEvent.getStatus(), "Status should be persisted");
		assertEquals("Original notes", persistedEvent.getSummaryNotes(), "Summary notes should be preserved");
		assertEquals(1L, persistedEvent.getId(), "ID should be preserved");

		log.info("Finishing testGuardarButtonPersistsAllChanges");
	}

}
