package com.nutriconsultas.calendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.nutriconsultas.dataTables.paging.Column;
import com.nutriconsultas.dataTables.paging.Direction;
import com.nutriconsultas.dataTables.paging.Order;
import com.nutriconsultas.dataTables.paging.PageArray;
import com.nutriconsultas.dataTables.paging.PagingRequest;
import com.nutriconsultas.dataTables.paging.Search;
import com.nutriconsultas.paciente.Paciente;

import lombok.extern.slf4j.Slf4j;

@ExtendWith(MockitoExtension.class)
@Slf4j
@ActiveProfiles("test")
public class CalendarEventRestControllerTest {

	@InjectMocks
	private CalendarEventRestController calendarEventRestController;

	@Mock
	private CalendarEventService service;

	private List<CalendarEvent> events;

	private Paciente paciente;

	@BeforeEach
	public void setup() {
		log.info("setting up calendar event service");

		// Create test paciente
		paciente = new Paciente();
		paciente.setId(1L);
		paciente.setName("Juan Perez");

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
		event2.setEventDateTime(new Date(System.currentTimeMillis() + 172800000)); // Day
																					// after
																					// tomorrow
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
		events.add(event3);

		when(service.findAll()).thenReturn(events);
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
		assertThat(result.getRecordsTotal()).isEqualTo(1);
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

}
