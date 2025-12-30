package com.nutriconsultas.paciente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.calendar.CalendarEventService;
import com.nutriconsultas.calendar.EventStatus;
import com.nutriconsultas.charts.ChartResponse;
import com.nutriconsultas.dataTables.paging.Direction;
import com.nutriconsultas.dataTables.paging.Order;
import com.nutriconsultas.dataTables.paging.PageArray;
import com.nutriconsultas.dataTables.paging.PagingRequest;
import com.nutriconsultas.dataTables.paging.Search;

import lombok.extern.slf4j.Slf4j;

@ExtendWith(MockitoExtension.class)
@Slf4j
@ActiveProfiles("test")
public class PacienteConsultaRestControllerTest {

	@InjectMocks
	private PacienteConsultaRestController pacienteConsultaRestController;

	@Mock
	private CalendarEventService calendarEventService;

	private Paciente paciente;

	private CalendarEvent consulta1;

	private CalendarEvent consulta2;

	@BeforeEach
	public void setup() {
		log.info("setting up PacienteConsultaRestController test");

		paciente = new Paciente();
		paciente.setId(1L);
		paciente.setName("Test Paciente");

		consulta1 = new CalendarEvent();
		consulta1.setId(1L);
		consulta1.setTitle("Consulta 1");
		consulta1.setEventDateTime(new Date(System.currentTimeMillis() - 86400000)); // Yesterday
		consulta1.setStatus(EventStatus.COMPLETED);
		consulta1.setDurationMinutes(60);
		consulta1.setPeso(70.0);
		consulta1.setEstatura(1.75);
		consulta1.setImc(22.86);
		consulta1.setIndiceGrasaCorporal(15.5);
		consulta1.setSistolica(120);
		consulta1.setDiastolica(80);
		consulta1.setIndiceGlucemico(95);
		consulta1.setPaciente(paciente);

		consulta2 = new CalendarEvent();
		consulta2.setId(2L);
		consulta2.setTitle("Consulta 2");
		consulta2.setEventDateTime(new Date()); // Today
		consulta2.setStatus(EventStatus.COMPLETED);
		consulta2.setDurationMinutes(30);
		consulta2.setPeso(71.0);
		consulta2.setEstatura(1.75);
		consulta2.setImc(23.18);
		consulta2.setIndiceGrasaCorporal(16.0);
		consulta2.setSistolica(125);
		consulta2.setDiastolica(85);
		consulta2.setIndiceGlucemico(100);
		consulta2.setPaciente(paciente);

		log.info("finished setting up PacienteConsultaRestController test");
	}

	@Test
	public void testGetPageArray() {
		log.info("Starting testGetPageArray");
		// Arrange
		when(calendarEventService.findByPacienteId(1L)).thenReturn(Arrays.asList(consulta1, consulta2));

		PagingRequest pagingRequest = new PagingRequest();
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setDraw(1);
		pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
		pagingRequest.setSearch(new Search("", "false"));

		// Act
		PageArray result = pacienteConsultaRestController.getPageArray(pagingRequest, 1L);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getRecordsTotal()).isEqualTo(2);
		assertThat(result.getRecordsFiltered()).isEqualTo(2);
		assertThat(result.getDraw()).isEqualTo(1);
		assertThat(result.getData()).isNotEmpty();
		assertThat(result.getData().size()).isEqualTo(2);
		verify(calendarEventService).findByPacienteId(1L);
		log.info("Finishing testGetPageArray");
	}

	@Test
	public void testGetPageArrayWithSearch() {
		log.info("Starting testGetPageArrayWithSearch");
		// Arrange
		when(calendarEventService.findByPacienteId(1L)).thenReturn(Arrays.asList(consulta1, consulta2));

		PagingRequest pagingRequest = new PagingRequest();
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setDraw(1);
		pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
		pagingRequest.setSearch(new Search("Consulta 1", "false"));

		// Act
		PageArray result = pacienteConsultaRestController.getPageArray(pagingRequest, 1L);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getRecordsTotal()).isEqualTo(2);
		assertThat(result.getRecordsFiltered()).isEqualTo(1);
		assertThat(result.getData()).isNotEmpty();
		log.info("Finishing testGetPageArrayWithSearch");
	}

	@Test
	public void testGetPageArrayNotImplemented() {
		log.info("Starting testGetPageArrayNotImplemented");
		// Arrange
		PagingRequest pagingRequest = new PagingRequest();

		// Act
		PageArray result = pacienteConsultaRestController.getPageArray(pagingRequest);

		// Assert
		assertThat(result).isNull();
		log.info("Finishing testGetPageArrayNotImplemented");
	}

	@Test
	public void testToStringList() {
		log.info("Starting testToStringList");
		// Act
		List<String> result = pacienteConsultaRestController.toStringList(consulta1);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(5);
		assertThat(result.get(0)).isNotEmpty(); // Date formatted
		assertThat(result.get(1)).contains("Consulta 1");
		assertThat(result.get(2)).isEqualTo("Completado");
		assertThat(result.get(3)).isEqualTo("60 min");
		assertThat(result.get(4)).contains("delete-btn");
		log.info("Finishing testToStringList");
	}

	@Test
	public void testToStringListWithNullValues() {
		log.info("Starting testToStringListWithNullValues");
		// Arrange
		CalendarEvent consulta = new CalendarEvent();
		consulta.setTitle("Test");

		// Act
		List<String> result = pacienteConsultaRestController.toStringList(consulta);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(5);
		assertThat(result.get(0)).isEqualTo("");
		assertThat(result.get(3)).isEqualTo("");
		log.info("Finishing testToStringListWithNullValues");
	}

	@Test
	public void testGetData() {
		log.info("Starting testGetData");
		// Arrange
		when(calendarEventService.findAll()).thenReturn(Arrays.asList(consulta1, consulta2));

		// Act
		List<CalendarEvent> result = pacienteConsultaRestController.getData();

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(2);
		verify(calendarEventService).findAll();
		log.info("Finishing testGetData");
	}

	@Test
	public void testGetRows() {
		log.info("Starting testGetRows");
		// Arrange
		when(calendarEventService.findByPacienteId(1L)).thenReturn(Arrays.asList(consulta1, consulta2));

		PagingRequest pagingRequest = new PagingRequest();
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setDraw(1);
		pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
		pagingRequest.setSearch(new Search("", "false"));
		pagingRequest.setColumns(pacienteConsultaRestController.getColumns());

		// Act
		var page = pacienteConsultaRestController.getRows(pagingRequest, 1L);

		// Assert
		assertThat(page).isNotNull();
		assertThat(page.getRecordsTotal()).isEqualTo(2);
		verify(calendarEventService).findByPacienteId(1L);
		log.info("Finishing testGetRows");
	}

	@Test
	public void testGetRowsWithoutPacienteId() {
		log.info("Starting testGetRowsWithoutPacienteId");
		// Arrange
		when(calendarEventService.findAll()).thenReturn(Arrays.asList(consulta1, consulta2));

		PagingRequest pagingRequest = new PagingRequest();
		pagingRequest.setStart(0);
		pagingRequest.setLength(10);
		pagingRequest.setDraw(1);
		pagingRequest.setOrder(Arrays.asList(new Order(0, Direction.asc)));
		pagingRequest.setSearch(new Search("", "false"));
		pagingRequest.setColumns(pacienteConsultaRestController.getColumns());

		// Act
		var page = pacienteConsultaRestController.getRows(pagingRequest);

		// Assert
		assertThat(page).isNotNull();
		assertThat(page.getRecordsTotal()).isEqualTo(2);
		verify(calendarEventService).findAll();
		log.info("Finishing testGetRowsWithoutPacienteId");
	}

	@Test
	public void testGetComparator() {
		log.info("Starting testGetComparator");
		// Test title comparator
		var titleComparator = pacienteConsultaRestController.getComparator("title", Direction.asc);
		assertThat(titleComparator).isNotNull();
		int result = titleComparator.compare(consulta1, consulta2);
		assertThat(result).isLessThan(0); // "Consulta 1" < "Consulta 2"

		// Test date comparator
		var dateComparator = pacienteConsultaRestController.getComparator("eventDateTime", Direction.asc);
		assertThat(dateComparator).isNotNull();
		int dateResult = dateComparator.compare(consulta1, consulta2);
		assertThat(dateResult).isLessThan(0); // Yesterday < Today

		// Test duration comparator
		var durationComparator = pacienteConsultaRestController.getComparator("duration", Direction.asc);
		assertThat(durationComparator).isNotNull();
		int durationResult = durationComparator.compare(consulta1, consulta2);
		assertThat(durationResult).isGreaterThan(0); // 60 > 30

		// Test status comparator
		var statusComparator = pacienteConsultaRestController.getComparator("status", Direction.asc);
		assertThat(statusComparator).isNotNull();

		// Test descending
		var descComparator = pacienteConsultaRestController.getComparator("title", Direction.desc);
		int descResult = descComparator.compare(consulta1, consulta2);
		assertThat(descResult).isGreaterThan(0);

		// Test default comparator
		var defaultComparator = pacienteConsultaRestController.getComparator("unknown", Direction.asc);
		assertThat(defaultComparator).isNotNull();
		log.info("Finishing testGetComparator");
	}

	@Test
	public void testGetColumns() {
		log.info("Starting testGetColumns");
		// Act
		var result = pacienteConsultaRestController.getColumns();

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(5);
		assertThat(result.get(0).getData()).isEqualTo("fecha");
		assertThat(result.get(1).getData()).isEqualTo("titulo");
		assertThat(result.get(2).getData()).isEqualTo("estado");
		assertThat(result.get(3).getData()).isEqualTo("duracion");
		assertThat(result.get(4).getData()).isEqualTo("actions");
		log.info("Finishing testGetColumns");
	}

	@Test
	public void testGetPredicate() {
		log.info("Starting testGetPredicate");
		// Test title match
		var predicate = pacienteConsultaRestController.getPredicate("Consulta 1");
		assertThat(predicate.test(consulta1)).isTrue();
		assertThat(predicate.test(consulta2)).isFalse();

		// Test status match
		var statusPredicate = pacienteConsultaRestController.getPredicate("Completado");
		assertThat(statusPredicate.test(consulta1)).isTrue();
		assertThat(statusPredicate.test(consulta2)).isTrue();

		// Test no match
		var noMatchPredicate = pacienteConsultaRestController.getPredicate("NonExistent");
		assertThat(noMatchPredicate.test(consulta1)).isFalse();
		log.info("Finishing testGetPredicate");
	}

	@Test
	public void testGetChartData() {
		log.info("Starting testGetChartData");
		// Arrange
		when(calendarEventService.findByPacienteId(1L)).thenReturn(Arrays.asList(consulta1, consulta2));

		// Act
		ChartResponse result = pacienteConsultaRestController.getChartData(1L, "peso");

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getLabels()).isNotNull();
		assertThat(result.getLabels().size()).isEqualTo(2);
		assertThat(result.getData()).isNotNull();
		assertThat(result.getData().containsKey("peso")).isTrue();
		assertThat(result.getData().containsKey("estatura")).isTrue();
		assertThat(result.getData().containsKey("imc")).isTrue();
		assertThat(result.getData().containsKey("grasaCorporal")).isTrue();
		assertThat(result.getData().containsKey("presionArterial")).isTrue();
		assertThat(result.getData().containsKey("sistolica")).isTrue();
		assertThat(result.getData().containsKey("diastolica")).isTrue();
		assertThat(result.getData().containsKey("indiceGlucemico")).isTrue();

		@SuppressWarnings("unchecked")
		List<Double> pesoData = (List<Double>) result.getData().get("peso");
		assertThat(pesoData).isNotNull();
		assertThat(pesoData.size()).isEqualTo(2);
		assertThat(pesoData.get(0)).isEqualTo(70.0);
		assertThat(pesoData.get(1)).isEqualTo(71.0);

		verify(calendarEventService).findByPacienteId(1L);
		log.info("Finishing testGetChartData");
	}

	@Test
	public void testGetChartDataWithNullValues() {
		log.info("Starting testGetChartDataWithNullValues");
		// Arrange
		CalendarEvent consulta = new CalendarEvent();
		consulta.setEventDateTime(new Date());
		when(calendarEventService.findByPacienteId(1L)).thenReturn(Arrays.asList(consulta));

		// Act
		ChartResponse result = pacienteConsultaRestController.getChartData(1L, "peso");

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getLabels()).isNotNull();
		assertThat(result.getData()).isNotNull();
		log.info("Finishing testGetChartDataWithNullValues");
	}

	@Test
	public void testGetChartDataWithNullEventDateTime() {
		log.info("Starting testGetChartDataWithNullEventDateTime");
		// Arrange
		CalendarEvent consulta = new CalendarEvent();
		consulta.setEventDateTime(null);
		when(calendarEventService.findByPacienteId(1L)).thenReturn(Arrays.asList(consulta));

		// Act
		ChartResponse result = pacienteConsultaRestController.getChartData(1L, "peso");

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getLabels()).isEmpty();
		log.info("Finishing testGetChartDataWithNullEventDateTime");
	}

}
