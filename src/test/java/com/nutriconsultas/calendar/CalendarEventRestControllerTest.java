package com.nutriconsultas.calendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.nutriconsultas.dataTables.paging.Column;
import com.nutriconsultas.dataTables.paging.Direction;
import com.nutriconsultas.dataTables.paging.Order;
import com.nutriconsultas.dataTables.paging.PageArray;
import com.nutriconsultas.dataTables.paging.PagingRequest;
import com.nutriconsultas.dataTables.paging.Search;
import com.nutriconsultas.paciente.BodyFatCalculatorService;
import com.nutriconsultas.paciente.NivelPeso;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;

import lombok.extern.slf4j.Slf4j;

@ExtendWith(MockitoExtension.class)
@Slf4j
@ActiveProfiles("test")
@SuppressWarnings("null") // Mockito matchers with @NonNull types generate false positives
public class CalendarEventRestControllerTest {

	@InjectMocks
	private CalendarEventRestController calendarEventRestController;

	@Mock
	private CalendarEventService service;

	@Mock
	private PacienteRepository pacienteRepository;

	@Mock
	private BodyFatCalculatorService bodyFatCalculatorService;

	private List<CalendarEvent> events;

	private Paciente paciente;

	private List<Paciente> pacientes;

	@BeforeEach
	public void setup() {
		log.info("setting up calendar event service");

		// Create test paciente
		paciente = new Paciente();
		paciente.setId(1L);
		paciente.setName("Juan Perez");
		// Set date of birth for age calculation tests
		final Calendar cal = Calendar.getInstance();
		cal.set(1990, Calendar.JANUARY, 15);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		paciente.setDob(cal.getTime());
		paciente.setGender("M");

		// Create test events
		events = new ArrayList<>();

		CalendarEvent event1 = new CalendarEvent();
		event1.setId(1L);
		event1.setTitle("Consulta de nutrición");
		event1.setDescription("Primera consulta");
		event1.setEventDateTime(new Date(System.currentTimeMillis() + 86400000)); // Tomorrow
		event1.setDurationMinutes(60);
		event1.setStatus(EventStatus.SCHEDULED);
		event1.setPaciente(paciente);
		events.add(event1);

		CalendarEvent event2 = new CalendarEvent();
		event2.setId(2L);
		event2.setTitle("Seguimiento");
		event2.setDescription("Seguimiento mensual");
		// Day after tomorrow
		event2.setEventDateTime(new Date(System.currentTimeMillis() + 172800000));
		event2.setDurationMinutes(30);
		event2.setStatus(EventStatus.SCHEDULED);
		event2.setPaciente(paciente);
		events.add(event2);

		CalendarEvent event3 = new CalendarEvent();
		event3.setId(3L);
		event3.setTitle("Consulta completada");
		event3.setEventDateTime(new Date(System.currentTimeMillis() - 86400000)); // Yesterday
		event3.setDurationMinutes(45);
		event3.setStatus(EventStatus.COMPLETED);
		event3.setPaciente(paciente);
		event3.setSummaryNotes("Consulta completada exitosamente. Paciente mostró mejoría.");
		events.add(event3);

		lenient().when(service.findAll()).thenReturn(events);

		// Create test pacientes list
		pacientes = new ArrayList<>();
		pacientes.add(paciente);
		final Paciente paciente2 = new Paciente();
		paciente2.setId(2L);
		paciente2.setName("Maria Garcia");
		pacientes.add(paciente2);
		lenient().when(pacienteRepository.findAll()).thenReturn(pacientes);

		log.info("finished setting up calendar event service with {} events", events.size());
	}

	@Test
	public void testArray() {
		log.info("starting testArray");
		// Arrange
		PagingRequest pagingRequest = new PagingRequest();

		List<Column> columnList = new ArrayList<>();
		columnList.add(new Column("0", "", true, true, new Search("", "false")));
		columnList.add(new Column("1", "", true, true, new Search("", "false")));
		columnList.add(new Column("2", "", true, true, new Search("", "false")));
		columnList.add(new Column("3", "", true, true, new Search("", "false")));
		columnList.add(new Column("4", "", true, true, new Search("", "false")));

		pagingRequest.setColumns(columnList);
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setDraw(1);
		pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
		pagingRequest.setSearch(new Search("", "false"));
		log.debug("arrange paging request {}.", pagingRequest);

		// Act
		PageArray result = calendarEventRestController.getPageArray(pagingRequest);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getRecordsTotal()).isEqualTo(3);
		assertThat(result.getRecordsFiltered()).isEqualTo(3);
		assertThat(result.getDraw()).isEqualTo(1);
		assertThat(result.getData()).isNotEmpty();
		assertThat(result.getData().size()).isEqualTo(3);
		log.info("finished testArray with records {}", result.getRecordsTotal());
	}

	@Test
	public void testArrayNoOrder() {
		log.info("starting testArrayNoOrder");
		// Arrange
		PagingRequest pagingRequest = new PagingRequest();
		List<Column> columnList = new ArrayList<>();
		columnList.add(new Column("0", "", true, true, new Search("", "false")));
		columnList.add(new Column("1", "", true, true, new Search("", "false")));
		columnList.add(new Column("2", "", true, true, new Search("", "false")));
		columnList.add(new Column("3", "", true, true, new Search("", "false")));
		columnList.add(new Column("4", "", true, true, new Search("", "false")));
		pagingRequest.setColumns(columnList);
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setDraw(1);
		pagingRequest.setSearch(new Search("", "false"));

		// Act
		PageArray result = calendarEventRestController.getPageArray(pagingRequest);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getRecordsTotal()).isEqualTo(3);
		assertThat(result.getRecordsFiltered()).isEqualTo(3);
		assertThat(result.getDraw()).isEqualTo(1);
		assertThat(result.getData()).isNotEmpty();
		assertThat(result.getData().size()).isEqualTo(3);
		log.info("finished testArrayNoOrder with records {}", result.getRecordsTotal());
	}

	@Test
	public void testArrayFiltering() {
		log.info("starting testArrayFiltering");
		// Arrange
		PagingRequest pagingRequest = new PagingRequest();
		List<Column> columnList = new ArrayList<>();
		columnList.add(new Column("0", "", true, true, new Search("", "false")));
		columnList.add(new Column("1", "", true, true, new Search("", "false")));
		columnList.add(new Column("2", "", true, true, new Search("", "false")));
		columnList.add(new Column("3", "", true, true, new Search("", "false")));
		columnList.add(new Column("4", "", true, true, new Search("", "false")));
		pagingRequest.setColumns(columnList);
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setDraw(1);
		pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
		pagingRequest.setSearch(new Search("nutrición", "false"));
		log.debug("arrange paging request {}.", pagingRequest);

		// Act
		PageArray result = calendarEventRestController.getPageArray(pagingRequest);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getRecordsTotal()).isEqualTo(3);
		assertThat(result.getRecordsFiltered()).isEqualTo(1);
		assertThat(result.getDraw()).isEqualTo(1);
		assertThat(result.getData()).isNotEmpty();
		assertThat(result.getData().size()).isEqualTo(1);
		log.info("finished testArrayFiltering with records {}", result.getRecordsTotal());
	}

	@Test
	public void testArrayNoData() {
		log.info("starting testArrayNoData");
		// Arrange
		when(service.findAll()).thenReturn(new ArrayList<>());

		PagingRequest pagingRequest = new PagingRequest();
		List<Column> columnList = new ArrayList<>();
		columnList.add(new Column("0", "", true, true, new Search("", "false")));
		columnList.add(new Column("1", "", true, true, new Search("", "false")));
		columnList.add(new Column("2", "", true, true, new Search("", "false")));
		columnList.add(new Column("3", "", true, true, new Search("", "false")));
		columnList.add(new Column("4", "", true, true, new Search("", "false")));
		pagingRequest.setColumns(columnList);
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setDraw(1);
		pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
		pagingRequest.setSearch(new Search("", "false"));
		log.debug("arrange paging request {}.", pagingRequest);

		// Act
		PageArray result = calendarEventRestController.getPageArray(pagingRequest);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getRecordsTotal()).isEqualTo(0);
		assertThat(result.getRecordsFiltered()).isEqualTo(0);
		assertThat(result.getDraw()).isEqualTo(1);
		assertThat(result.getData()).isEmpty();
		log.info("finished testArrayNoData with records {}", result.getRecordsTotal());
	}

	@Test
	public void testArrayFilteringByPaciente() {
		log.info("starting testArrayFilteringByPaciente");
		// Arrange
		PagingRequest pagingRequest = new PagingRequest();
		List<Column> columnList = new ArrayList<>();
		columnList.add(new Column("0", "", true, true, new Search("", "false")));
		columnList.add(new Column("1", "", true, true, new Search("", "false")));
		columnList.add(new Column("2", "", true, true, new Search("", "false")));
		columnList.add(new Column("3", "", true, true, new Search("", "false")));
		columnList.add(new Column("4", "", true, true, new Search("", "false")));
		pagingRequest.setColumns(columnList);
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setDraw(1);
		pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
		pagingRequest.setSearch(new Search("Juan", "false"));
		log.debug("arrange paging request {}.", pagingRequest);

		// Act
		PageArray result = calendarEventRestController.getPageArray(pagingRequest);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getRecordsTotal()).isEqualTo(3);
		assertThat(result.getRecordsFiltered()).isEqualTo(3);
		assertThat(result.getDraw()).isEqualTo(1);
		assertThat(result.getData()).isNotEmpty();
		log.info("finished testArrayFilteringByPaciente with records {}", result.getRecordsTotal());
	}

	@Test
	public void testArraySortingByTitle() {
		log.info("starting testArraySortingByTitle");
		// Arrange
		PagingRequest pagingRequest = new PagingRequest();
		List<Column> columnList = new ArrayList<>();
		columnList.add(new Column("0", "title", true, true, new Search("", "false")));
		columnList.add(new Column("1", "", true, true, new Search("", "false")));
		columnList.add(new Column("2", "", true, true, new Search("", "false")));
		columnList.add(new Column("3", "", true, true, new Search("", "false")));
		columnList.add(new Column("4", "", true, true, new Search("", "false")));
		pagingRequest.setColumns(columnList);
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setDraw(1);
		pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
		pagingRequest.setSearch(new Search("", "false"));
		log.debug("arrange paging request {}.", pagingRequest);

		// Act
		PageArray result = calendarEventRestController.getPageArray(pagingRequest);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getRecordsTotal()).isEqualTo(3);
		assertThat(result.getRecordsFiltered()).isEqualTo(3);
		assertThat(result.getDraw()).isEqualTo(1);
		assertThat(result.getData()).isNotEmpty();
		log.info("finished testArraySortingByTitle with records {}", result.getRecordsTotal());
	}

	@Test
	public void testGetCalendarEventsWithoutDateRange() {
		log.info("starting testGetCalendarEventsWithoutDateRange");
		// Act
		final List<Map<String, Object>> result = calendarEventRestController.getCalendarEvents(null, null);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(3);
		verify(service).findAll();
		log.info("finished testGetCalendarEventsWithoutDateRange with {} events", result.size());
	}

	@Test
	public void testGetCalendarEventsWithDateRange() {
		log.info("starting testGetCalendarEventsWithDateRange");
		// Arrange
		// 2 days ago
		final Date startDate = new Date(System.currentTimeMillis() - 172800000);
		// 3 days from now
		final Date endDate = new Date(System.currentTimeMillis() + 259200000);
		final List<CalendarEvent> filteredEvents = new ArrayList<>();
		filteredEvents.add(events.get(0)); // Tomorrow's event
		filteredEvents.add(events.get(1)); // Day after tomorrow's event
		when(service.findEventsBetweenDates(startDate, endDate)).thenReturn(filteredEvents);

		// Act
		final List<Map<String, Object>> result = calendarEventRestController.getCalendarEvents(startDate, endDate);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(2);
		verify(service).findEventsBetweenDates(startDate, endDate);
		log.info("finished testGetCalendarEventsWithDateRange with {} events", result.size());
	}

	@Test
	public void testCalendarEventFormat() {
		log.info("starting testCalendarEventFormat");
		// Act
		final List<Map<String, Object>> result = calendarEventRestController.getCalendarEvents(null, null);

		// Assert
		assertThat(result).isNotEmpty();
		final Map<String, Object> eventMap = result.get(0);
		assertThat(eventMap).containsKeys("id", "title", "start", "end", "allDay", "extendedProps", "url");
		assertThat(eventMap.get("id")).isEqualTo("1");
		assertThat(eventMap.get("title")).isEqualTo("Consulta de nutrición");
		assertThat(eventMap.get("allDay")).isEqualTo(false);
		assertThat(eventMap.get("url")).isEqualTo("/admin/calendario/1");
		assertThat(eventMap.get("start")).isNotNull();
		assertThat(eventMap.get("end")).isNotNull();
		log.info("finished testCalendarEventFormat");
	}

	@Test
	public void testCalendarEventExtendedProps() {
		log.info("starting testCalendarEventExtendedProps");
		// Act
		final List<Map<String, Object>> result = calendarEventRestController.getCalendarEvents(null, null);

		// Assert
		assertThat(result).isNotEmpty();
		final Map<String, Object> eventMap = result.get(0);
		@SuppressWarnings("unchecked")
		final Map<String, Object> extendedProps = (Map<String, Object>) eventMap.get("extendedProps");
		assertThat(extendedProps).isNotNull();
		assertThat(extendedProps).containsKeys("paciente", "pacienteId", "status", "description", "durationMinutes");
		assertThat(extendedProps.get("paciente")).isEqualTo("Juan Perez");
		assertThat(extendedProps.get("pacienteId")).isEqualTo(1L);
		assertThat(extendedProps.get("status")).isEqualTo("SCHEDULED");
		assertThat(extendedProps.get("description")).isEqualTo("Primera consulta");
		assertThat(extendedProps.get("durationMinutes")).isEqualTo(60);
		log.info("finished testCalendarEventExtendedProps");
	}

	@Test
	public void testCalendarEventExtendedPropsWithSummaryNotes() {
		log.info("starting testCalendarEventExtendedPropsWithSummaryNotes");
		// Act
		final List<Map<String, Object>> result = calendarEventRestController.getCalendarEvents(null, null);

		// Assert - Check event3 which has summaryNotes
		assertThat(result.size()).isGreaterThanOrEqualTo(3);
		final Map<String, Object> eventMap = result.get(2); // event3
		@SuppressWarnings("unchecked")
		final Map<String, Object> extendedProps = (Map<String, Object>) eventMap.get("extendedProps");
		assertThat(extendedProps).isNotNull();
		assertThat(extendedProps).containsKey("summaryNotes");
		assertThat(extendedProps.get("summaryNotes"))
			.isEqualTo("Consulta completada exitosamente. Paciente mostró mejoría.");
		log.info("finished testCalendarEventExtendedPropsWithSummaryNotes");
	}

	@Test
	public void testCalendarEventExtendedPropsWithoutSummaryNotes() {
		log.info("starting testCalendarEventExtendedPropsWithoutSummaryNotes");
		// Act
		final List<Map<String, Object>> result = calendarEventRestController.getCalendarEvents(null, null);

		// Assert - Check event1 which doesn't have summaryNotes
		assertThat(result.size()).isGreaterThanOrEqualTo(1);
		final Map<String, Object> eventMap = result.get(0); // event1
		@SuppressWarnings("unchecked")
		final Map<String, Object> extendedProps = (Map<String, Object>) eventMap.get("extendedProps");
		assertThat(extendedProps).isNotNull();
		// summaryNotes should not be present if null
		if (extendedProps.containsKey("summaryNotes")) {
			assertThat(extendedProps.get("summaryNotes")).isNull();
		}
		log.info("finished testCalendarEventExtendedPropsWithoutSummaryNotes");
	}

	@Test
	public void testGetEventByIdSuccess() {
		log.info("starting testGetEventByIdSuccess");
		// Arrange
		final CalendarEvent event = events.get(0);
		when(service.findById(1L)).thenReturn(event);

		// Act
		final ResponseEntity<Map<String, Object>> response = calendarEventRestController.getEvent(1L);

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		final Map<String, Object> responseBody = response.getBody();
		assertThat(responseBody).isNotNull();
		Objects.requireNonNull(responseBody);
		assertThat(responseBody.get("success")).isEqualTo(true);
		assertThat(responseBody).containsKey("event");
		@SuppressWarnings("unchecked")
		final Map<String, Object> eventMap = (Map<String, Object>) responseBody.get("event");
		assertThat(eventMap).isNotNull();
		assertThat(eventMap.get("id")).isEqualTo("1");
		assertThat(eventMap.get("title")).isEqualTo("Consulta de nutrición");
		verify(service).findById(1L);
		log.info("finished testGetEventByIdSuccess");
	}

	@Test
	public void testGetEventByIdNotFound() {
		log.info("starting testGetEventByIdNotFound");
		// Arrange
		when(service.findById(999L)).thenReturn(null);

		// Act
		final ResponseEntity<Map<String, Object>> response = calendarEventRestController.getEvent(999L);

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		final Map<String, Object> responseBody = response.getBody();
		assertThat(responseBody).isNotNull();
		Objects.requireNonNull(responseBody);
		assertThat(responseBody.get("success")).isEqualTo(false);
		assertThat(responseBody.get("error")).isEqualTo("Event not found");
		verify(service).findById(999L);
		log.info("finished testGetEventByIdNotFound");
	}

	@Test
	public void testGetEventByIdWithSummaryNotes() {
		log.info("starting testGetEventByIdWithSummaryNotes");
		// Arrange
		final CalendarEvent event = events.get(2); // event3 has summaryNotes
		when(service.findById(3L)).thenReturn(event);

		// Act
		final ResponseEntity<Map<String, Object>> response = calendarEventRestController.getEvent(3L);

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		final Map<String, Object> responseBody = response.getBody();
		assertThat(responseBody).isNotNull();
		Objects.requireNonNull(responseBody);
		@SuppressWarnings("unchecked")
		final Map<String, Object> eventMap = (Map<String, Object>) responseBody.get("event");
		@SuppressWarnings("unchecked")
		final Map<String, Object> extendedProps = (Map<String, Object>) eventMap.get("extendedProps");
		assertThat(extendedProps).containsKey("summaryNotes");
		assertThat(extendedProps.get("summaryNotes"))
			.isEqualTo("Consulta completada exitosamente. Paciente mostró mejoría.");
		log.info("finished testGetEventByIdWithSummaryNotes");
	}

	@Test
	public void testGetEventByIdWithServiceException() {
		log.info("starting testGetEventByIdWithServiceException");
		// Arrange
		when(service.findById(1L)).thenThrow(new RuntimeException("Database error"));

		// Act
		final ResponseEntity<Map<String, Object>> response = calendarEventRestController.getEvent(1L);

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		final Map<String, Object> responseBody = response.getBody();
		assertThat(responseBody).isNotNull();
		Objects.requireNonNull(responseBody);
		assertThat(responseBody.get("success")).isEqualTo(false);
		assertThat(responseBody).containsKey("error");
		log.info("finished testGetEventByIdWithServiceException");
	}

	@Test
	public void testUpdateEventStatus() {
		log.info("starting testUpdateEventStatus");
		// Arrange
		final CalendarEvent existingEvent = events.get(0);
		final CalendarEvent updatedEvent = new CalendarEvent();
		updatedEvent.setId(1L);
		updatedEvent.setTitle(existingEvent.getTitle());
		updatedEvent.setDescription(existingEvent.getDescription());
		updatedEvent.setEventDateTime(existingEvent.getEventDateTime());
		updatedEvent.setDurationMinutes(existingEvent.getDurationMinutes());
		updatedEvent.setStatus(EventStatus.COMPLETED);
		updatedEvent.setPaciente(existingEvent.getPaciente());

		final Map<String, Object> eventData = new HashMap<>();
		eventData.put("status", "COMPLETED");

		when(service.findById(1L)).thenReturn(existingEvent);
		Objects.requireNonNull(updatedEvent);
		when(service.save(any(CalendarEvent.class))).thenReturn(updatedEvent);

		// Act
		final ResponseEntity<Map<String, Object>> response = calendarEventRestController.updateEvent(1L, eventData);

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		final Map<String, Object> responseBody = response.getBody();
		assertThat(responseBody).isNotNull();
		Objects.requireNonNull(responseBody);
		assertThat(responseBody.get("success")).isEqualTo(true);
		assertThat(responseBody).containsKey("event");
		verify(service).findById(1L);
		verify(service).save(any(CalendarEvent.class));
		log.info("finished testUpdateEventStatus");
	}

	@Test
	public void testUpdateEventSummaryNotes() {
		log.info("starting testUpdateEventSummaryNotes");
		// Arrange
		final CalendarEvent existingEvent = events.get(0);
		final CalendarEvent updatedEvent = new CalendarEvent();
		updatedEvent.setId(1L);
		updatedEvent.setTitle(existingEvent.getTitle());
		updatedEvent.setDescription(existingEvent.getDescription());
		updatedEvent.setEventDateTime(existingEvent.getEventDateTime());
		updatedEvent.setDurationMinutes(existingEvent.getDurationMinutes());
		updatedEvent.setStatus(existingEvent.getStatus());
		updatedEvent.setPaciente(existingEvent.getPaciente());
		updatedEvent.setSummaryNotes("Nuevas notas de resumen de la consulta.");

		final Map<String, Object> eventData = new HashMap<>();
		eventData.put("summaryNotes", "Nuevas notas de resumen de la consulta.");

		when(service.findById(1L)).thenReturn(existingEvent);
		Objects.requireNonNull(updatedEvent);
		when(service.save(any(CalendarEvent.class))).thenReturn(updatedEvent);

		// Act
		final ResponseEntity<Map<String, Object>> response = calendarEventRestController.updateEvent(1L, eventData);

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		final Map<String, Object> responseBody = response.getBody();
		assertThat(responseBody).isNotNull();
		Objects.requireNonNull(responseBody);
		assertThat(responseBody.get("success")).isEqualTo(true);
		@SuppressWarnings("unchecked")
		final Map<String, Object> eventMap = (Map<String, Object>) responseBody.get("event");
		@SuppressWarnings("unchecked")
		final Map<String, Object> extendedProps = (Map<String, Object>) eventMap.get("extendedProps");
		assertThat(extendedProps.get("summaryNotes")).isEqualTo("Nuevas notas de resumen de la consulta.");
		verify(service).findById(1L);
		verify(service).save(any(CalendarEvent.class));
		log.info("finished testUpdateEventSummaryNotes");
	}

	@Test
	public void testUpdateEventStatusAndSummaryNotes() {
		log.info("starting testUpdateEventStatusAndSummaryNotes");
		// Arrange
		final CalendarEvent existingEvent = events.get(0);
		final CalendarEvent updatedEvent = new CalendarEvent();
		updatedEvent.setId(1L);
		updatedEvent.setTitle(existingEvent.getTitle());
		updatedEvent.setDescription(existingEvent.getDescription());
		updatedEvent.setEventDateTime(existingEvent.getEventDateTime());
		updatedEvent.setDurationMinutes(existingEvent.getDurationMinutes());
		updatedEvent.setStatus(EventStatus.COMPLETED);
		updatedEvent.setPaciente(existingEvent.getPaciente());
		updatedEvent.setSummaryNotes("Consulta completada con éxito.");

		final Map<String, Object> eventData = new HashMap<>();
		eventData.put("status", "COMPLETED");
		eventData.put("summaryNotes", "Consulta completada con éxito.");

		when(service.findById(1L)).thenReturn(existingEvent);
		Objects.requireNonNull(updatedEvent);
		when(service.save(any(CalendarEvent.class))).thenReturn(updatedEvent);

		// Act
		final ResponseEntity<Map<String, Object>> response = calendarEventRestController.updateEvent(1L, eventData);

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		final Map<String, Object> responseBody = response.getBody();
		assertThat(responseBody).isNotNull();
		Objects.requireNonNull(responseBody);
		assertThat(responseBody.get("success")).isEqualTo(true);
		verify(service).findById(1L);
		verify(service).save(any(CalendarEvent.class));
		log.info("finished testUpdateEventStatusAndSummaryNotes");
	}

	@Test
	public void testUpdateEventNotFound() {
		log.info("starting testUpdateEventNotFound");
		// Arrange
		final Map<String, Object> eventData = new HashMap<>();
		eventData.put("status", "COMPLETED");

		when(service.findById(999L)).thenReturn(null);

		// Act
		final ResponseEntity<Map<String, Object>> response = calendarEventRestController.updateEvent(999L, eventData);

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		final Map<String, Object> responseBody = response.getBody();
		assertThat(responseBody).isNotNull();
		Objects.requireNonNull(responseBody);
		assertThat(responseBody.get("success")).isEqualTo(false);
		assertThat(responseBody.get("error")).isEqualTo("Event not found");
		verify(service).findById(999L);
		verify(service, never()).save(any(CalendarEvent.class));
		log.info("finished testUpdateEventNotFound");
	}

	@Test
	public void testUpdateEventWithInvalidStatus() {
		log.info("starting testUpdateEventWithInvalidStatus");
		// Arrange
		final CalendarEvent existingEvent = events.get(0);
		final Map<String, Object> eventData = new HashMap<>();
		eventData.put("status", "INVALID_STATUS");

		when(service.findById(1L)).thenReturn(existingEvent);

		// Act
		final ResponseEntity<Map<String, Object>> response = calendarEventRestController.updateEvent(1L, eventData);

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		final Map<String, Object> responseBody = response.getBody();
		assertThat(responseBody).isNotNull();
		Objects.requireNonNull(responseBody);
		assertThat(responseBody.get("success")).isEqualTo(false);
		assertThat(responseBody).containsKey("error");
		verify(service).findById(1L);
		log.info("finished testUpdateEventWithInvalidStatus");
	}

	@Test
	public void testUpdateEventWithServiceException() {
		log.info("starting testUpdateEventWithServiceException");
		// Arrange
		final CalendarEvent existingEvent = events.get(0);
		final Map<String, Object> eventData = new HashMap<>();
		eventData.put("status", "COMPLETED");

		when(service.findById(1L)).thenReturn(existingEvent);
		when(service.save(any(CalendarEvent.class))).thenThrow(new RuntimeException("Database error"));

		// Act
		final ResponseEntity<Map<String, Object>> response = calendarEventRestController.updateEvent(1L, eventData);

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		final Map<String, Object> responseBody = response.getBody();
		assertThat(responseBody).isNotNull();
		Objects.requireNonNull(responseBody);
		assertThat(responseBody.get("success")).isEqualTo(false);
		assertThat(responseBody).containsKey("error");
		verify(service).findById(1L);
		verify(service).save(any(CalendarEvent.class));
		log.info("finished testUpdateEventWithServiceException");
	}

	@Test
	public void testUpdateEventWithNoChanges() {
		log.info("starting testUpdateEventWithNoChanges");
		// Arrange
		final CalendarEvent existingEvent = events.get(0);
		final Map<String, Object> eventData = new HashMap<>();
		// Empty eventData - no changes

		when(service.findById(1L)).thenReturn(existingEvent);
		Objects.requireNonNull(existingEvent);
		when(service.save(any(CalendarEvent.class))).thenReturn(existingEvent);

		// Act
		final ResponseEntity<Map<String, Object>> response = calendarEventRestController.updateEvent(1L, eventData);

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		final Map<String, Object> responseBody = response.getBody();
		assertThat(responseBody).isNotNull();
		Objects.requireNonNull(responseBody);
		assertThat(responseBody.get("success")).isEqualTo(true);
		verify(service).findById(1L);
		verify(service).save(any(CalendarEvent.class));
		log.info("finished testUpdateEventWithNoChanges");
	}

	@Test
	public void testUpdateEventWithAllStatuses() {
		log.info("starting testUpdateEventWithAllStatuses");
		// Test updating to all three status values
		final String[] statuses = {"SCHEDULED", "COMPLETED", "CANCELLED"};

		for (final String status : statuses) {
			// Arrange
			final CalendarEvent existingEvent = events.get(0);
			final CalendarEvent updatedEvent = new CalendarEvent();
			updatedEvent.setId(1L);
			updatedEvent.setTitle(existingEvent.getTitle());
			updatedEvent.setDescription(existingEvent.getDescription());
			updatedEvent.setEventDateTime(existingEvent.getEventDateTime());
			updatedEvent.setDurationMinutes(existingEvent.getDurationMinutes());
			updatedEvent.setStatus(EventStatus.valueOf(status));
			updatedEvent.setPaciente(existingEvent.getPaciente());

			final Map<String, Object> eventData = new HashMap<>();
			eventData.put("status", status);

			when(service.findById(1L)).thenReturn(existingEvent);
			Objects.requireNonNull(updatedEvent);
			when(service.save(any(CalendarEvent.class))).thenReturn(updatedEvent);

			// Act
			final ResponseEntity<Map<String, Object>> response = calendarEventRestController.updateEvent(1L, eventData);

			// Assert
			assertThat(response).isNotNull();
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			final Map<String, Object> responseBody = response.getBody();
			assertThat(responseBody).isNotNull();
			Objects.requireNonNull(responseBody);
			assertThat(responseBody.get("success")).isEqualTo(true);
		}
		log.info("finished testUpdateEventWithAllStatuses");
	}

	@Test
	public void testCalendarEventEndTimeCalculation() {
		log.info("starting testCalendarEventEndTimeCalculation");
		// Act
		final List<Map<String, Object>> result = calendarEventRestController.getCalendarEvents(null, null);

		// Assert
		assertThat(result).isNotEmpty();
		final Map<String, Object> eventMap = result.get(0);
		final String startStr = (String) eventMap.get("start");
		final String endStr = (String) eventMap.get("end");

		// Parse dates and verify end is start + duration
		final java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		try {
			final Date startDate = format.parse(startStr);
			final Date endDate = format.parse(endStr);
			final long actualDuration = endDate.getTime() - startDate.getTime();
			final long expectedDuration = events.get(0).getDurationMinutes() * 60 * 1000L;
			assertThat(actualDuration).isEqualTo(expectedDuration);
		}
		catch (final java.text.ParseException e) {
			throw new AssertionError("Failed to parse date", e);
		}
		log.info("finished testCalendarEventEndTimeCalculation");
	}

	@Test
	public void testCalendarEventWithNullDuration() {
		log.info("starting testCalendarEventWithNullDuration");
		// Arrange
		final CalendarEvent eventWithNullDuration = new CalendarEvent();
		eventWithNullDuration.setId(4L);
		eventWithNullDuration.setTitle("Evento sin duración");
		eventWithNullDuration.setEventDateTime(new Date());
		eventWithNullDuration.setDurationMinutes(null);
		eventWithNullDuration.setStatus(EventStatus.SCHEDULED);
		eventWithNullDuration.setPaciente(paciente);
		final List<CalendarEvent> eventsWithNullDuration = new ArrayList<>();
		eventsWithNullDuration.add(eventWithNullDuration);
		when(service.findAll()).thenReturn(eventsWithNullDuration);

		// Act
		final List<Map<String, Object>> result = calendarEventRestController.getCalendarEvents(null, null);

		// Assert
		assertThat(result).isNotEmpty();
		final Map<String, Object> eventMap = result.get(0);
		assertThat(eventMap).containsKey("start");
		assertThat(eventMap).doesNotContainKey("end");
		log.info("finished testCalendarEventWithNullDuration");
	}

	@Test
	public void testCalendarEventWithZeroDuration() {
		log.info("starting testCalendarEventWithZeroDuration");
		// Arrange
		final CalendarEvent eventWithZeroDuration = new CalendarEvent();
		eventWithZeroDuration.setId(5L);
		eventWithZeroDuration.setTitle("Evento sin duración");
		eventWithZeroDuration.setEventDateTime(new Date());
		eventWithZeroDuration.setDurationMinutes(0);
		eventWithZeroDuration.setStatus(EventStatus.SCHEDULED);
		eventWithZeroDuration.setPaciente(paciente);
		final List<CalendarEvent> eventsWithZeroDuration = new ArrayList<>();
		eventsWithZeroDuration.add(eventWithZeroDuration);
		when(service.findAll()).thenReturn(eventsWithZeroDuration);

		// Act
		final List<Map<String, Object>> result = calendarEventRestController.getCalendarEvents(null, null);

		// Assert
		assertThat(result).isNotEmpty();
		final Map<String, Object> eventMap = result.get(0);
		assertThat(eventMap).containsKey("start");
		assertThat(eventMap).doesNotContainKey("end");
		log.info("finished testCalendarEventWithZeroDuration");
	}

	@Test
	public void testCalendarEventsEmptyList() {
		log.info("starting testCalendarEventsEmptyList");
		// Arrange
		when(service.findAll()).thenReturn(new ArrayList<>());

		// Act
		final List<Map<String, Object>> result = calendarEventRestController.getCalendarEvents(null, null);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).isEmpty();
		log.info("finished testCalendarEventsEmptyList");
	}

	@Test
	public void testGetPacientes() {
		log.info("starting testGetPacientes");
		// Act
		final List<Map<String, Object>> result = calendarEventRestController.getPacientes();

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(2);
		verify(pacienteRepository).findAll();

		// Verify first paciente
		final Map<String, Object> paciente1 = result.get(0);
		assertThat(paciente1).containsKeys("id", "name");
		assertThat(paciente1.get("id")).isEqualTo(1L);
		assertThat(paciente1.get("name")).isEqualTo("Juan Perez");

		// Verify second paciente
		final Map<String, Object> paciente2 = result.get(1);
		assertThat(paciente2).containsKeys("id", "name");
		assertThat(paciente2.get("id")).isEqualTo(2L);
		assertThat(paciente2.get("name")).isEqualTo("Maria Garcia");

		log.info("finished testGetPacientes with {} pacientes", result.size());
	}

	@Test
	public void testGetPacientesEmptyList() {
		log.info("starting testGetPacientesEmptyList");
		// Arrange
		when(pacienteRepository.findAll()).thenReturn(new ArrayList<>());

		// Act
		final List<Map<String, Object>> result = calendarEventRestController.getPacientes();

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).isEmpty();
		verify(pacienteRepository).findAll();
		log.info("finished testGetPacientesEmptyList");
	}

	@Test
	public void testGetNextAvailableTimeWithNoEvents() {
		log.info("starting testGetNextAvailableTimeWithNoEvents");
		// Arrange - Use a future date
		final Calendar futureCal = Calendar.getInstance();
		futureCal.add(Calendar.DAY_OF_MONTH, 7); // 7 days from now
		final String dateStr = String.format("%04d-%02d-%02d", futureCal.get(Calendar.YEAR),
				futureCal.get(Calendar.MONTH) + 1, futureCal.get(Calendar.DAY_OF_MONTH));
		// NOSONAR - Mockito matcher
		when(service.findEventsBetweenDates(any(Date.class), any(Date.class))).thenReturn(new ArrayList<>());

		// Act
		final Map<String, Object> result = calendarEventRestController.getNextAvailableTime(dateStr);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.get("available")).isEqualTo(true);
		assertThat(result).containsKey("dateTime");
		verify(service).findEventsBetweenDates(any(Date.class), any(Date.class));
		log.info("finished testGetNextAvailableTimeWithNoEvents");
	}

	@Test
	public void testGetNextAvailableTimeWithEvents() {
		log.info("starting testGetNextAvailableTimeWithEvents");
		// Arrange - Use a future date
		final Calendar futureCal = Calendar.getInstance();
		futureCal.add(Calendar.DAY_OF_MONTH, 7); // 7 days from now
		futureCal.set(Calendar.HOUR_OF_DAY, 0);
		futureCal.set(Calendar.MINUTE, 0);
		futureCal.set(Calendar.SECOND, 0);
		futureCal.set(Calendar.MILLISECOND, 0);
		final String dateStr = String.format("%04d-%02d-%02d", futureCal.get(Calendar.YEAR),
				futureCal.get(Calendar.MONTH) + 1, futureCal.get(Calendar.DAY_OF_MONTH));
		final List<CalendarEvent> existingEvents = new ArrayList<>();

		// Create an event at 9 AM (8-9 AM should be available)
		final Calendar cal = Calendar.getInstance();
		cal.setTime(futureCal.getTime());
		cal.set(Calendar.HOUR_OF_DAY, 9);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);

		final CalendarEvent event9AM = new CalendarEvent();
		event9AM.setId(1L);
		event9AM.setTitle("Event at 9 AM");
		event9AM.setEventDateTime(cal.getTime());
		event9AM.setDurationMinutes(60);
		event9AM.setStatus(EventStatus.SCHEDULED);
		existingEvents.add(event9AM);

		// NOSONAR - Mockito matcher
		when(service.findEventsBetweenDates(any(Date.class), any(Date.class))).thenReturn(existingEvents);

		// Act
		final Map<String, Object> result = calendarEventRestController.getNextAvailableTime(dateStr);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.get("available")).isEqualTo(true);
		assertThat(result).containsKey("dateTime");
		// Should return 8 AM as it's the first available hour
		final String dateTime = (String) result.get("dateTime");
		assertThat(dateTime).contains("08:00:00");
		verify(service).findEventsBetweenDates(any(Date.class), any(Date.class));
		log.info("finished testGetNextAvailableTimeWithEvents");
	}

	@Test
	public void testGetNextAvailableTimeWithFullDay() {
		log.info("starting testGetNextAvailableTimeWithFullDay");
		// Arrange - Use a future date and create events for every hour from 8 AM to 5 PM
		final Calendar futureCal = Calendar.getInstance();
		futureCal.add(Calendar.DAY_OF_MONTH, 7); // 7 days from now
		futureCal.set(Calendar.HOUR_OF_DAY, 0);
		futureCal.set(Calendar.MINUTE, 0);
		futureCal.set(Calendar.SECOND, 0);
		futureCal.set(Calendar.MILLISECOND, 0);
		final String dateStr = String.format("%04d-%02d-%02d", futureCal.get(Calendar.YEAR),
				futureCal.get(Calendar.MONTH) + 1, futureCal.get(Calendar.DAY_OF_MONTH));
		final List<CalendarEvent> existingEvents = new ArrayList<>();

		for (int hour = 8; hour <= 17; hour++) {
			final Calendar cal = Calendar.getInstance();
			cal.setTime(futureCal.getTime());
			cal.set(Calendar.HOUR_OF_DAY, hour);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);

			final CalendarEvent event = new CalendarEvent();
			event.setId((long) hour);
			event.setTitle("Event at " + hour + " AM");
			event.setEventDateTime(cal.getTime());
			event.setDurationMinutes(60);
			event.setStatus(EventStatus.SCHEDULED);
			existingEvents.add(event);
		}

		// NOSONAR - Mockito matcher
		when(service.findEventsBetweenDates(any(Date.class), any(Date.class))).thenReturn(existingEvents);

		// Act
		final Map<String, Object> result = calendarEventRestController.getNextAvailableTime(dateStr);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.get("available")).isEqualTo(false);
		assertThat(result).doesNotContainKey("dateTime");
		verify(service).findEventsBetweenDates(any(Date.class), any(Date.class));
		log.info("finished testGetNextAvailableTimeWithFullDay");
	}

	@Test
	public void testGetNextAvailableTimeWithInvalidDate() {
		log.info("starting testGetNextAvailableTimeWithInvalidDate");
		// Arrange
		final String invalidDateStr = "invalid-date";

		// Act
		final Map<String, Object> result = calendarEventRestController.getNextAvailableTime(invalidDateStr);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.get("available")).isEqualTo(false);
		assertThat(result).containsKey("error");
		assertThat(result.get("error")).isEqualTo("Invalid date format");
		log.info("finished testGetNextAvailableTimeWithInvalidDate");
	}

	@Test
	public void testGetNextAvailableTimeWithPartialDay() {
		log.info("starting testGetNextAvailableTimeWithPartialDay");
		// Arrange - Use a future date and create events at 8 AM, 10 AM, 12 PM (9 AM, 11
		// AM should be available)
		final Calendar futureCal = Calendar.getInstance();
		futureCal.add(Calendar.DAY_OF_MONTH, 7); // 7 days from now
		futureCal.set(Calendar.HOUR_OF_DAY, 0);
		futureCal.set(Calendar.MINUTE, 0);
		futureCal.set(Calendar.SECOND, 0);
		futureCal.set(Calendar.MILLISECOND, 0);
		final String dateStr = String.format("%04d-%02d-%02d", futureCal.get(Calendar.YEAR),
				futureCal.get(Calendar.MONTH) + 1, futureCal.get(Calendar.DAY_OF_MONTH));
		final List<CalendarEvent> existingEvents = new ArrayList<>();

		final int[] hours = {8, 10, 12};
		for (final int hour : hours) {
			final Calendar cal = Calendar.getInstance();
			cal.setTime(futureCal.getTime());
			cal.set(Calendar.HOUR_OF_DAY, hour);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);

			final CalendarEvent event = new CalendarEvent();
			event.setId((long) hour);
			event.setTitle("Event at " + hour + " AM");
			event.setEventDateTime(cal.getTime());
			event.setDurationMinutes(60);
			event.setStatus(EventStatus.SCHEDULED);
			existingEvents.add(event);
		}

		// NOSONAR - Mockito matcher
		when(service.findEventsBetweenDates(any(Date.class), any(Date.class))).thenReturn(existingEvents);

		// Act
		final Map<String, Object> result = calendarEventRestController.getNextAvailableTime(dateStr);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.get("available")).isEqualTo(true);
		assertThat(result).containsKey("dateTime");
		// Should return 9 AM as it's the first available hour
		final String dateTime = (String) result.get("dateTime");
		assertThat(dateTime).contains("09:00:00");
		verify(service).findEventsBetweenDates(any(Date.class), any(Date.class));
		log.info("finished testGetNextAvailableTimeWithPartialDay");
	}

	@Test
	public void testGetNextAvailableTimeWithOverlappingEvents() {
		log.info("starting testGetNextAvailableTimeWithOverlappingEvents");
		// Arrange - Use a future date and create overlapping events
		final Calendar futureCal = Calendar.getInstance();
		futureCal.add(Calendar.DAY_OF_MONTH, 7); // 7 days from now
		futureCal.set(Calendar.HOUR_OF_DAY, 0);
		futureCal.set(Calendar.MINUTE, 0);
		futureCal.set(Calendar.SECOND, 0);
		futureCal.set(Calendar.MILLISECOND, 0);
		final String dateStr = String.format("%04d-%02d-%02d", futureCal.get(Calendar.YEAR),
				futureCal.get(Calendar.MONTH) + 1, futureCal.get(Calendar.DAY_OF_MONTH));
		final List<CalendarEvent> existingEvents = new ArrayList<>();

		// Event from 8:00 to 9:30
		final Calendar cal1 = Calendar.getInstance();
		cal1.setTime(futureCal.getTime());
		cal1.set(Calendar.HOUR_OF_DAY, 8);
		cal1.set(Calendar.MINUTE, 0);
		cal1.set(Calendar.SECOND, 0);
		cal1.set(Calendar.MILLISECOND, 0);
		final CalendarEvent event1 = new CalendarEvent();
		event1.setId(1L);
		event1.setTitle("Event 1");
		event1.setEventDateTime(cal1.getTime());
		event1.setDurationMinutes(90); // 1.5 hours
		event1.setStatus(EventStatus.SCHEDULED);
		existingEvents.add(event1);

		// Event from 9:30 to 10:30
		final Calendar cal2 = Calendar.getInstance();
		cal2.setTime(futureCal.getTime());
		cal2.set(Calendar.HOUR_OF_DAY, 9);
		cal2.set(Calendar.MINUTE, 30);
		cal2.set(Calendar.SECOND, 0);
		cal2.set(Calendar.MILLISECOND, 0);
		final CalendarEvent event2 = new CalendarEvent();
		event2.setId(2L);
		event2.setTitle("Event 2");
		event2.setEventDateTime(cal2.getTime());
		event2.setDurationMinutes(60);
		event2.setStatus(EventStatus.SCHEDULED);
		existingEvents.add(event2);

		// NOSONAR - Mockito matcher
		when(service.findEventsBetweenDates(any(Date.class), any(Date.class))).thenReturn(existingEvents);

		// Act
		final Map<String, Object> result = calendarEventRestController.getNextAvailableTime(dateStr);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.get("available")).isEqualTo(true);
		assertThat(result).containsKey("dateTime");
		// Should return 10:30 or later as 8-10:30 is blocked
		final String dateTime = (String) result.get("dateTime");
		// Verify it's after 10:30
		assertThat(dateTime).isNotNull();
		verify(service).findEventsBetweenDates(any(Date.class), any(Date.class));
		log.info("finished testGetNextAvailableTimeWithOverlappingEvents");
	}

	@Test
	public void testGetNextAvailableTimeWithPastDate() {
		log.info("starting testGetNextAvailableTimeWithPastDate");
		// Arrange - Use a past date (yesterday)
		final Calendar pastCal = Calendar.getInstance();
		pastCal.add(Calendar.DAY_OF_MONTH, -1); // Yesterday
		pastCal.set(Calendar.HOUR_OF_DAY, 0);
		pastCal.set(Calendar.MINUTE, 0);
		pastCal.set(Calendar.SECOND, 0);
		pastCal.set(Calendar.MILLISECOND, 0);
		final String dateStr = String.format("%04d-%02d-%02d", pastCal.get(Calendar.YEAR),
				pastCal.get(Calendar.MONTH) + 1, pastCal.get(Calendar.DAY_OF_MONTH));

		// Act
		final Map<String, Object> result = calendarEventRestController.getNextAvailableTime(dateStr);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.get("available")).isEqualTo(true);
		assertThat(result).containsKey("dateTime");
		assertThat(result.get("isPastDate")).isEqualTo(true);
		// Should return default time (8:00 AM) without calculating availability
		final String dateTime = (String) result.get("dateTime");
		assertThat(dateTime).contains("08:00:00");
		// Verify that findEventsBetweenDates was NOT called (no availability calculation)
		// NOSONAR - Mockito matcher
		verify(service, never()).findEventsBetweenDates(any(Date.class), any(Date.class));
		log.info("finished testGetNextAvailableTimeWithPastDate");
	}

	@Test
	public void testGetNextAvailableTimeWithPastDateMultipleDaysAgo() {
		log.info("starting testGetNextAvailableTimeWithPastDateMultipleDaysAgo");
		// Arrange - Use a date several days in the past
		final Calendar pastCal = Calendar.getInstance();
		pastCal.add(Calendar.DAY_OF_MONTH, -7); // 7 days ago
		pastCal.set(Calendar.HOUR_OF_DAY, 0);
		pastCal.set(Calendar.MINUTE, 0);
		pastCal.set(Calendar.SECOND, 0);
		pastCal.set(Calendar.MILLISECOND, 0);
		final String dateStr = String.format("%04d-%02d-%02d", pastCal.get(Calendar.YEAR),
				pastCal.get(Calendar.MONTH) + 1, pastCal.get(Calendar.DAY_OF_MONTH));

		// Act
		final Map<String, Object> result = calendarEventRestController.getNextAvailableTime(dateStr);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.get("available")).isEqualTo(true);
		assertThat(result).containsKey("dateTime");
		assertThat(result.get("isPastDate")).isEqualTo(true);
		// Should return default time (8:00 AM) for the past date
		final String dateTime = (String) result.get("dateTime");
		assertThat(dateTime).contains("08:00:00");
		// Verify the date matches the requested past date
		final String expectedDatePart = String.format("%04d-%02d-%02d", pastCal.get(Calendar.YEAR),
				pastCal.get(Calendar.MONTH) + 1, pastCal.get(Calendar.DAY_OF_MONTH));
		assertThat(dateTime).startsWith(expectedDatePart);
		// Verify that findEventsBetweenDates was NOT called (no availability calculation)
		// NOSONAR - Mockito matcher
		verify(service, never()).findEventsBetweenDates(any(Date.class), any(Date.class));
		log.info("finished testGetNextAvailableTimeWithPastDateMultipleDaysAgo");
	}

	@Test
	public void testGetNextAvailableTimeWithPastDateDoesNotCheckAvailability() {
		log.info("starting testGetNextAvailableTimeWithPastDateDoesNotCheckAvailability");
		// Arrange - Use a past date and verify that even if events exist, they are not
		// checked
		final Calendar pastCal = Calendar.getInstance();
		pastCal.add(Calendar.DAY_OF_MONTH, -2); // 2 days ago
		pastCal.set(Calendar.HOUR_OF_DAY, 0);
		pastCal.set(Calendar.MINUTE, 0);
		pastCal.set(Calendar.SECOND, 0);
		pastCal.set(Calendar.MILLISECOND, 0);
		final String dateStr = String.format("%04d-%02d-%02d", pastCal.get(Calendar.YEAR),
				pastCal.get(Calendar.MONTH) + 1, pastCal.get(Calendar.DAY_OF_MONTH));

		// Create some events that would block availability if checked
		final List<CalendarEvent> blockingEvents = new ArrayList<>();
		for (int hour = 8; hour <= 17; hour++) {
			final Calendar cal = Calendar.getInstance();
			cal.setTime(pastCal.getTime());
			cal.set(Calendar.HOUR_OF_DAY, hour);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			final CalendarEvent event = new CalendarEvent();
			event.setId((long) hour);
			event.setTitle("Blocking Event");
			event.setEventDateTime(cal.getTime());
			event.setDurationMinutes(60);
			event.setStatus(EventStatus.SCHEDULED);
			blockingEvents.add(event);
		}
		// NOSONAR - Mockito matcher
		lenient().when(service.findEventsBetweenDates(any(Date.class), any(Date.class))).thenReturn(blockingEvents);

		// Act
		final Map<String, Object> result = calendarEventRestController.getNextAvailableTime(dateStr);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.get("available")).isEqualTo(true);
		assertThat(result).containsKey("dateTime");
		assertThat(result.get("isPastDate")).isEqualTo(true);
		// Should return default time (8:00 AM) even though events would block it
		final String dateTime = (String) result.get("dateTime");
		assertThat(dateTime).contains("08:00:00");
		// Verify that findEventsBetweenDates was NOT called (no availability calculation
		// for past dates)
		// NOSONAR - Mockito matcher
		verify(service, never()).findEventsBetweenDates(any(Date.class), any(Date.class));
		log.info("finished testGetNextAvailableTimeWithPastDateDoesNotCheckAvailability");
	}

	@Test
	public void testSaveEventSuccess() {
		log.info("starting testSaveEventSuccess");
		// Arrange
		final Map<String, Object> eventData = new HashMap<>();
		eventData.put("pacienteId", "1");
		eventData.put("title", "Nueva Consulta");
		eventData.put("description", "Descripción de prueba");
		eventData.put("eventDateTime", "2024-12-31T10:00");
		eventData.put("durationMinutes", "60");
		eventData.put("status", "SCHEDULED");

		final CalendarEvent savedEvent = new CalendarEvent();
		savedEvent.setId(1L);
		savedEvent.setTitle("Nueva Consulta");
		savedEvent.setDescription("Descripción de prueba");
		savedEvent.setDurationMinutes(60);
		savedEvent.setStatus(EventStatus.SCHEDULED);
		savedEvent.setPaciente(paciente);
		final Calendar cal = Calendar.getInstance();
		cal.set(2024, Calendar.DECEMBER, 31, 10, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		savedEvent.setEventDateTime(cal.getTime());

		when(pacienteRepository.findById(1L)).thenReturn(java.util.Optional.of(paciente));
		Objects.requireNonNull(savedEvent);
		Objects.requireNonNull(savedEvent);
		when(service.save(any(CalendarEvent.class))).thenReturn(savedEvent);

		// Act
		final ResponseEntity<Map<String, Object>> response = calendarEventRestController.saveEvent(eventData);

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		final Map<String, Object> responseBody = response.getBody();
		assertThat(responseBody).isNotNull();
		Objects.requireNonNull(responseBody);
		assertThat(responseBody.get("success")).isEqualTo(true);
		assertThat(responseBody).containsKey("event");
		verify(pacienteRepository).findById(1L);
		verify(service).save(any(CalendarEvent.class));
		log.info("finished testSaveEventSuccess");
	}

	@Test
	public void testSaveEventWithDefaultValues() {
		log.info("starting testSaveEventWithDefaultValues");
		// Arrange - missing durationMinutes and status should use defaults
		final Map<String, Object> eventData = new HashMap<>();
		eventData.put("pacienteId", "1");
		eventData.put("title", "Nueva Consulta");
		eventData.put("eventDateTime", "2024-12-31T10:00");

		final CalendarEvent savedEvent = new CalendarEvent();
		savedEvent.setId(1L);
		savedEvent.setTitle("Nueva Consulta");
		savedEvent.setDurationMinutes(60); // Default
		savedEvent.setStatus(EventStatus.SCHEDULED); // Default
		savedEvent.setPaciente(paciente);
		final Calendar cal = Calendar.getInstance();
		cal.set(2024, Calendar.DECEMBER, 31, 10, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		savedEvent.setEventDateTime(cal.getTime());

		when(pacienteRepository.findById(1L)).thenReturn(java.util.Optional.of(paciente));
		Objects.requireNonNull(savedEvent);
		Objects.requireNonNull(savedEvent);
		when(service.save(any(CalendarEvent.class))).thenReturn(savedEvent);

		// Act
		final ResponseEntity<Map<String, Object>> response = calendarEventRestController.saveEvent(eventData);

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		final Map<String, Object> responseBody = response.getBody();
		assertThat(responseBody).isNotNull();
		Objects.requireNonNull(responseBody);
		assertThat(responseBody.get("success")).isEqualTo(true);
		verify(service).save(any(CalendarEvent.class));
		log.info("finished testSaveEventWithDefaultValues");
	}

	@Test
	public void testSaveEventWithInvalidPacienteId() {
		log.info("starting testSaveEventWithInvalidPacienteId");
		// Arrange
		final Map<String, Object> eventData = new HashMap<>();
		eventData.put("pacienteId", "999");
		eventData.put("title", "Nueva Consulta");
		eventData.put("eventDateTime", "2024-12-31T10:00");

		when(pacienteRepository.findById(999L)).thenReturn(java.util.Optional.empty());

		// Act
		final ResponseEntity<Map<String, Object>> response = calendarEventRestController.saveEvent(eventData);

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		final Map<String, Object> responseBody = response.getBody();
		assertThat(responseBody).isNotNull();
		Objects.requireNonNull(responseBody);
		assertThat(responseBody.get("success")).isEqualTo(false);
		assertThat(responseBody).containsKey("error");
		verify(pacienteRepository).findById(999L);
		verify(service, never()).save(any(CalendarEvent.class));
		log.info("finished testSaveEventWithInvalidPacienteId");
	}

	@Test
	public void testSaveEventWithInvalidDateFormat() {
		log.info("starting testSaveEventWithInvalidDateFormat");
		// Arrange
		final Map<String, Object> eventData = new HashMap<>();
		eventData.put("pacienteId", "1");
		eventData.put("title", "Nueva Consulta");
		eventData.put("eventDateTime", "invalid-date-format");

		lenient().when(pacienteRepository.findById(1L)).thenReturn(java.util.Optional.of(paciente));

		// Act
		final ResponseEntity<Map<String, Object>> response = calendarEventRestController.saveEvent(eventData);

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		final Map<String, Object> responseBody = response.getBody();
		assertThat(responseBody).isNotNull();
		Objects.requireNonNull(responseBody);
		assertThat(responseBody.get("success")).isEqualTo(false);
		assertThat(responseBody).containsKey("error");
		// pacienteRepository.findById may or may not be called depending on when the date
		// parsing fails
		verify(service, never()).save(any(CalendarEvent.class));
		log.info("finished testSaveEventWithInvalidDateFormat");
	}

	@Test
	public void testSaveEventWithDifferentDateFormats() {
		log.info("starting testSaveEventWithDifferentDateFormats");
		// Test various date formats that should be accepted
		final String[] dateFormats = {"2024-12-31T10:00", "2024-12-31T10:00:00", "2024-12-31 10:00",
				"2024-12-31 10:00:00"};

		for (final String dateFormat : dateFormats) {
			// Arrange
			final Map<String, Object> eventData = new HashMap<>();
			eventData.put("pacienteId", "1");
			eventData.put("title", "Nueva Consulta");
			eventData.put("eventDateTime", dateFormat);

			final CalendarEvent savedEvent = new CalendarEvent();
			savedEvent.setId(1L);
			savedEvent.setTitle("Nueva Consulta");
			savedEvent.setDurationMinutes(60);
			savedEvent.setStatus(EventStatus.SCHEDULED);
			savedEvent.setPaciente(paciente);
			final Calendar cal = Calendar.getInstance();
			cal.set(2024, Calendar.DECEMBER, 31, 10, 0, 0);
			cal.set(Calendar.MILLISECOND, 0);
			savedEvent.setEventDateTime(cal.getTime());

			when(pacienteRepository.findById(1L)).thenReturn(java.util.Optional.of(paciente));
			Objects.requireNonNull(savedEvent);
			Objects.requireNonNull(savedEvent);
			when(service.save(any(CalendarEvent.class))).thenReturn(savedEvent);

			// Act
			final ResponseEntity<Map<String, Object>> response = calendarEventRestController.saveEvent(eventData);

			// Assert
			assertThat(response).isNotNull();
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			final Map<String, Object> responseBody = response.getBody();
			assertThat(responseBody).isNotNull();
			Objects.requireNonNull(responseBody);
			assertThat(responseBody.get("success")).isEqualTo(true);
		}
		log.info("finished testSaveEventWithDifferentDateFormats");
	}

	@Test
	public void testSaveEventWithAllStatuses() {
		log.info("starting testSaveEventWithAllStatuses");
		// Test all three status values
		final String[] statuses = {"SCHEDULED", "COMPLETED", "CANCELLED"};

		for (final String status : statuses) {
			// Arrange
			final Map<String, Object> eventData = new HashMap<>();
			eventData.put("pacienteId", "1");
			eventData.put("title", "Nueva Consulta");
			eventData.put("eventDateTime", "2024-12-31T10:00");
			eventData.put("status", status);

			final CalendarEvent savedEvent = new CalendarEvent();
			savedEvent.setId(1L);
			savedEvent.setTitle("Nueva Consulta");
			savedEvent.setDurationMinutes(60);
			savedEvent.setStatus(EventStatus.valueOf(status));
			savedEvent.setPaciente(paciente);
			final Calendar cal = Calendar.getInstance();
			cal.set(2024, Calendar.DECEMBER, 31, 10, 0, 0);
			cal.set(Calendar.MILLISECOND, 0);
			savedEvent.setEventDateTime(cal.getTime());

			when(pacienteRepository.findById(1L)).thenReturn(java.util.Optional.of(paciente));
			Objects.requireNonNull(savedEvent);
			Objects.requireNonNull(savedEvent);
			when(service.save(any(CalendarEvent.class))).thenReturn(savedEvent);

			// Act
			final ResponseEntity<Map<String, Object>> response = calendarEventRestController.saveEvent(eventData);

			// Assert
			assertThat(response).isNotNull();
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			final Map<String, Object> responseBody = response.getBody();
			assertThat(responseBody).isNotNull();
			Objects.requireNonNull(responseBody);
			assertThat(responseBody.get("success")).isEqualTo(true);
		}
		log.info("finished testSaveEventWithAllStatuses");
	}

	@Test
	public void testSaveEventWithoutPacienteId() {
		log.info("starting testSaveEventWithoutPacienteId");
		// Arrange - pacienteId is optional in the REST endpoint
		final Map<String, Object> eventData = new HashMap<>();
		eventData.put("title", "Nueva Consulta");
		eventData.put("eventDateTime", "2024-12-31T10:00");
		eventData.put("durationMinutes", "60");
		eventData.put("status", "SCHEDULED");

		final CalendarEvent savedEvent = new CalendarEvent();
		savedEvent.setId(1L);
		savedEvent.setTitle("Nueva Consulta");
		savedEvent.setDurationMinutes(60);
		savedEvent.setStatus(EventStatus.SCHEDULED);
		final Calendar cal = Calendar.getInstance();
		cal.set(2024, Calendar.DECEMBER, 31, 10, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		savedEvent.setEventDateTime(cal.getTime());

		Objects.requireNonNull(savedEvent);
		Objects.requireNonNull(savedEvent);
		when(service.save(any(CalendarEvent.class))).thenReturn(savedEvent);

		// Act
		final ResponseEntity<Map<String, Object>> response = calendarEventRestController.saveEvent(eventData);

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		final Map<String, Object> responseBody = response.getBody();
		assertThat(responseBody).isNotNull();
		Objects.requireNonNull(responseBody);
		assertThat(responseBody.get("success")).isEqualTo(true);
		verify(service).save(any(CalendarEvent.class));
		log.info("finished testSaveEventWithoutPacienteId");
	}

	@Test
	public void testSaveEventWithServiceException() {
		log.info("starting testSaveEventWithServiceException");
		// Arrange
		final Map<String, Object> eventData = new HashMap<>();
		eventData.put("pacienteId", "1");
		eventData.put("title", "Nueva Consulta");
		eventData.put("eventDateTime", "2024-12-31T10:00");

		when(pacienteRepository.findById(1L)).thenReturn(java.util.Optional.of(paciente));
		when(service.save(any(CalendarEvent.class))).thenThrow(new RuntimeException("Database error"));

		// Act
		final ResponseEntity<Map<String, Object>> response = calendarEventRestController.saveEvent(eventData);

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		final Map<String, Object> responseBody = response.getBody();
		assertThat(responseBody).isNotNull();
		Objects.requireNonNull(responseBody);
		assertThat(responseBody.get("success")).isEqualTo(false);
		assertThat(responseBody).containsKey("error");
		log.info("finished testSaveEventWithServiceException");
	}

	@Test
	public void testUpdateEventWithPesoAndEstaturaCalculatesIMC() {
		log.info("starting testUpdateEventWithPesoAndEstaturaCalculatesIMC");
		// Arrange
		final CalendarEvent existingEvent = events.get(0);
		existingEvent.setPaciente(paciente);

		final Map<String, Object> eventData = new HashMap<>();
		eventData.put("peso", "75.5");
		eventData.put("estatura", "1.75");

		final CalendarEvent updatedEvent = new CalendarEvent();
		updatedEvent.setId(1L);
		updatedEvent.setPeso(75.5);
		updatedEvent.setEstatura(1.75);
		updatedEvent.setImc(24.69); // 75.5 / (1.75^2) = 24.69
		updatedEvent.setNivelPeso(NivelPeso.ALTO);
		updatedEvent.setPaciente(paciente);

		when(service.findById(1L)).thenReturn(existingEvent);
		lenient()
			.when(bodyFatCalculatorService.calculateBodyFatPercentage(any(Double.class), any(Integer.class),
					any(String.class)))
			.thenReturn(20.5);
		when(service.save(any(CalendarEvent.class))).thenAnswer(invocation -> {
			final CalendarEvent event = invocation.getArgument(0);
			// Copy the calculated values to updatedEvent for verification
			updatedEvent.setImc(event.getImc());
			updatedEvent.setNivelPeso(event.getNivelPeso());
			updatedEvent.setIndiceGrasaCorporal(event.getIndiceGrasaCorporal());
			return updatedEvent;
		});

		// Act
		final ResponseEntity<Map<String, Object>> response = calendarEventRestController.updateEvent(1L, eventData);

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		final Map<String, Object> responseBody = response.getBody();
		assertThat(responseBody).isNotNull();
		Objects.requireNonNull(responseBody);
		assertThat(responseBody.get("success")).isEqualTo(true);
		verify(service).findById(1L);
		verify(bodyFatCalculatorService).calculateBodyFatPercentage(any(Double.class), any(Integer.class),
				any(String.class));
		verify(service).save(any(CalendarEvent.class));
		log.info("finished testUpdateEventWithPesoAndEstaturaCalculatesIMC");
	}

	@Test
	public void testUpdateEventWithConsultationData() {
		log.info("starting testUpdateEventWithConsultationData");
		// Arrange
		final CalendarEvent existingEvent = events.get(0);
		existingEvent.setPaciente(paciente);

		final Map<String, Object> eventData = new HashMap<>();
		eventData.put("peso", "70.0");
		eventData.put("estatura", "1.70");
		eventData.put("sistolica", "120");
		eventData.put("diastolica", "80");
		eventData.put("indiceGlucemico", "95");
		eventData.put("summaryNotes", "Consulta de seguimiento");

		final CalendarEvent updatedEvent = new CalendarEvent();
		updatedEvent.setId(1L);
		updatedEvent.setPeso(70.0);
		updatedEvent.setEstatura(1.70);
		updatedEvent.setImc(24.22); // 70.0 / (1.70^2) = 24.22
		updatedEvent.setNivelPeso(NivelPeso.ALTO);
		updatedEvent.setSistolica(120);
		updatedEvent.setDiastolica(80);
		updatedEvent.setIndiceGlucemico(95);
		updatedEvent.setSummaryNotes("Consulta de seguimiento");
		updatedEvent.setPaciente(paciente);

		when(service.findById(1L)).thenReturn(existingEvent);
		lenient()
			.when(bodyFatCalculatorService.calculateBodyFatPercentage(any(Double.class), any(Integer.class),
					any(String.class)))
			.thenReturn(18.5);
		when(service.save(any(CalendarEvent.class))).thenAnswer(invocation -> {
			final CalendarEvent event = invocation.getArgument(0);
			updatedEvent.setPeso(event.getPeso());
			updatedEvent.setEstatura(event.getEstatura());
			updatedEvent.setImc(event.getImc());
			updatedEvent.setNivelPeso(event.getNivelPeso());
			updatedEvent.setIndiceGrasaCorporal(event.getIndiceGrasaCorporal());
			updatedEvent.setSistolica(event.getSistolica());
			updatedEvent.setDiastolica(event.getDiastolica());
			updatedEvent.setIndiceGlucemico(event.getIndiceGlucemico());
			updatedEvent.setSummaryNotes(event.getSummaryNotes());
			return updatedEvent;
		});

		// Act
		final ResponseEntity<Map<String, Object>> response = calendarEventRestController.updateEvent(1L, eventData);

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		final Map<String, Object> responseBody = response.getBody();
		assertThat(responseBody).isNotNull();
		Objects.requireNonNull(responseBody);
		assertThat(responseBody.get("success")).isEqualTo(true);
		verify(service).findById(1L);
		verify(bodyFatCalculatorService).calculateBodyFatPercentage(any(Double.class), any(Integer.class),
				any(String.class));
		verify(service).save(any(CalendarEvent.class));
		log.info("finished testUpdateEventWithConsultationData");
	}

	@Test
	public void testUpdateEventWithExplicitIMCAndBodyFat() {
		log.info("starting testUpdateEventWithExplicitIMCAndBodyFat");
		// Arrange - When IMC and body fat are explicitly provided, they should be used
		final CalendarEvent existingEvent = events.get(0);
		existingEvent.setPaciente(paciente);

		final Map<String, Object> eventData = new HashMap<>();
		eventData.put("imc", "25.5");
		eventData.put("indiceGrasaCorporal", "22.0");

		final CalendarEvent updatedEvent = new CalendarEvent();
		updatedEvent.setId(1L);
		updatedEvent.setImc(25.5);
		updatedEvent.setIndiceGrasaCorporal(22.0);
		updatedEvent.setPaciente(paciente);

		when(service.findById(1L)).thenReturn(existingEvent);
		when(service.save(any(CalendarEvent.class))).thenAnswer(invocation -> {
			final CalendarEvent event = invocation.getArgument(0);
			updatedEvent.setImc(event.getImc());
			updatedEvent.setIndiceGrasaCorporal(event.getIndiceGrasaCorporal());
			return updatedEvent;
		});

		// Act
		final ResponseEntity<Map<String, Object>> response = calendarEventRestController.updateEvent(1L, eventData);

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		final Map<String, Object> responseBody = response.getBody();
		assertThat(responseBody).isNotNull();
		Objects.requireNonNull(responseBody);
		assertThat(responseBody.get("success")).isEqualTo(true);
		verify(service).findById(1L);
		// Should not calculate body fat when explicitly provided (no peso/estatura
		// provided)
		verify(bodyFatCalculatorService, never()).calculateBodyFatPercentage(any(Double.class), any(Integer.class),
				any(String.class));
		verify(service).save(any(CalendarEvent.class));
		log.info("finished testUpdateEventWithExplicitIMCAndBodyFat");
	}

	@Test
	public void testUpdateEventWithPesoAndEstaturaButNoPatientData() {
		log.info("starting testUpdateEventWithPesoAndEstaturaButNoPatientData");
		// Arrange - Event without patient DOB or gender
		final CalendarEvent existingEvent = events.get(0);
		final Paciente pacienteSinDatos = new Paciente();
		pacienteSinDatos.setId(2L);
		pacienteSinDatos.setName("Paciente Sin Datos");
		existingEvent.setPaciente(pacienteSinDatos);

		final Map<String, Object> eventData = new HashMap<>();
		eventData.put("peso", "80.0");
		eventData.put("estatura", "1.80");

		final CalendarEvent updatedEvent = new CalendarEvent();
		updatedEvent.setId(1L);
		updatedEvent.setPeso(80.0);
		updatedEvent.setEstatura(1.80);
		updatedEvent.setImc(24.69); // 80.0 / (1.80^2) = 24.69
		updatedEvent.setNivelPeso(NivelPeso.ALTO);
		updatedEvent.setPaciente(pacienteSinDatos);

		when(service.findById(1L)).thenReturn(existingEvent);
		when(service.save(any(CalendarEvent.class))).thenAnswer(invocation -> {
			final CalendarEvent event = invocation.getArgument(0);
			updatedEvent.setPeso(event.getPeso());
			updatedEvent.setEstatura(event.getEstatura());
			updatedEvent.setImc(event.getImc());
			updatedEvent.setNivelPeso(event.getNivelPeso());
			return updatedEvent;
		});

		// Act
		final ResponseEntity<Map<String, Object>> response = calendarEventRestController.updateEvent(1L, eventData);

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		final Map<String, Object> responseBody = response.getBody();
		assertThat(responseBody).isNotNull();
		Objects.requireNonNull(responseBody);
		assertThat(responseBody.get("success")).isEqualTo(true);
		verify(service).findById(1L);
		// Should not calculate body fat when patient data is missing
		verify(bodyFatCalculatorService, never()).calculateBodyFatPercentage(any(Double.class), any(Integer.class),
				any(String.class));
		verify(service).save(any(CalendarEvent.class));
		log.info("finished testUpdateEventWithPesoAndEstaturaButNoPatientData");
	}

	@Test
	public void testUpdateEventWithOnlyPeso() {
		log.info("starting testUpdateEventWithOnlyPeso");
		// Arrange - Only peso provided, no estatura, so IMC should not be calculated
		final CalendarEvent existingEvent = events.get(0);
		existingEvent.setPaciente(paciente);

		final Map<String, Object> eventData = new HashMap<>();
		eventData.put("peso", "75.0");

		final CalendarEvent updatedEvent = new CalendarEvent();
		updatedEvent.setId(1L);
		updatedEvent.setPeso(75.0);
		updatedEvent.setPaciente(paciente);

		when(service.findById(1L)).thenReturn(existingEvent);
		when(service.save(any(CalendarEvent.class))).thenAnswer(invocation -> {
			final CalendarEvent event = invocation.getArgument(0);
			updatedEvent.setPeso(event.getPeso());
			return updatedEvent;
		});

		// Act
		final ResponseEntity<Map<String, Object>> response = calendarEventRestController.updateEvent(1L, eventData);

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		verify(service).findById(1L);
		// Should not calculate IMC or body fat when estatura is missing
		verify(bodyFatCalculatorService, never()).calculateBodyFatPercentage(any(Double.class), any(Integer.class),
				any(String.class));
		verify(service).save(any(CalendarEvent.class));
		log.info("finished testUpdateEventWithOnlyPeso");
	}

	@Test
	public void testUpdateEventWithSqlDate() {
		log.info("starting testUpdateEventWithSqlDate");
		// Arrange - Test that calculateAge handles java.sql.Date correctly
		final CalendarEvent existingEvent = events.get(0);
		final Paciente pacienteConSqlDate = new Paciente();
		pacienteConSqlDate.setId(3L);
		pacienteConSqlDate.setName("Paciente SQL Date");
		// Use java.sql.Date instead of java.util.Date
		final Calendar calSql = Calendar.getInstance();
		calSql.set(1990, Calendar.JANUARY, 15);
		calSql.set(Calendar.HOUR_OF_DAY, 0);
		calSql.set(Calendar.MINUTE, 0);
		calSql.set(Calendar.SECOND, 0);
		calSql.set(Calendar.MILLISECOND, 0);
		final java.sql.Date sqlDate = new java.sql.Date(calSql.getTimeInMillis());
		pacienteConSqlDate.setDob(sqlDate);
		pacienteConSqlDate.setGender("F");
		existingEvent.setPaciente(pacienteConSqlDate);

		final Map<String, Object> eventData = new HashMap<>();
		eventData.put("peso", "65.0");
		eventData.put("estatura", "1.65");

		final CalendarEvent updatedEvent = new CalendarEvent();
		updatedEvent.setId(1L);
		updatedEvent.setPeso(65.0);
		updatedEvent.setEstatura(1.65);
		updatedEvent.setImc(23.88); // 65.0 / (1.65^2) = 23.88
		updatedEvent.setNivelPeso(NivelPeso.NORMAL);
		updatedEvent.setPaciente(pacienteConSqlDate);

		when(service.findById(1L)).thenReturn(existingEvent);
		lenient()
			.when(bodyFatCalculatorService.calculateBodyFatPercentage(any(Double.class), any(Integer.class),
					any(String.class)))
			.thenReturn(25.0);
		when(service.save(any(CalendarEvent.class))).thenAnswer(invocation -> {
			final CalendarEvent event = invocation.getArgument(0);
			updatedEvent.setPeso(event.getPeso());
			updatedEvent.setEstatura(event.getEstatura());
			updatedEvent.setImc(event.getImc());
			updatedEvent.setNivelPeso(event.getNivelPeso());
			updatedEvent.setIndiceGrasaCorporal(event.getIndiceGrasaCorporal());
			return updatedEvent;
		});

		// Act
		final ResponseEntity<Map<String, Object>> response = calendarEventRestController.updateEvent(1L, eventData);

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		verify(service).findById(1L);
		// Should successfully calculate body fat even with java.sql.Date
		verify(bodyFatCalculatorService).calculateBodyFatPercentage(any(Double.class), any(Integer.class),
				any(String.class));
		verify(service).save(any(CalendarEvent.class));
		log.info("finished testUpdateEventWithSqlDate");
	}

}
