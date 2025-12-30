package com.nutriconsultas.paciente;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.calendar.CalendarEventService;
import com.nutriconsultas.calendar.EventStatus;
import com.nutriconsultas.controller.AbstractGridController;
import com.nutriconsultas.dataTables.paging.Column;
import com.nutriconsultas.dataTables.paging.Direction;
import com.nutriconsultas.dataTables.paging.Page;
import com.nutriconsultas.dataTables.paging.PageArray;
import com.nutriconsultas.dataTables.paging.PagingRequest;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/pacientes/{id}/consultas")
@Slf4j
public class PacienteConsultaRestController extends AbstractGridController<CalendarEvent> {

	@Autowired
	private CalendarEventService calendarEventService;

	@PostMapping("data-table")
	public PageArray getPageArray(@RequestBody final PagingRequest pagingRequest,
			@PathVariable @NonNull final Long id) {
		log.info("starting getPageArray with pagingRequest: {} for paciente id: {}", pagingRequest, id);
		pagingRequest.setColumns(getColumns());
		final Page<CalendarEvent> page = getRows(pagingRequest, id);
		log.debug("page with records: {}", page.getRecordsTotal());
		final PageArray pageArray = new PageArray();
		pageArray.setData(page.getData().stream().map(this::toStringList).collect(Collectors.toList()));
		pageArray.setDraw(page.getDraw());
		pageArray.setRecordsFiltered(page.getRecordsFiltered());
		pageArray.setRecordsTotal(page.getRecordsTotal());
		log.info("returning data at getPageArray: {}", pageArray.getRecordsTotal());
		return pageArray;
	}

	@Override
	@PostMapping("not-implemented")
	public PageArray getPageArray(@RequestBody final PagingRequest pagingRequest) {
		log.warn("getPageArray() called on PacienteConsultaRestController without id. This should not be called.");
		return null;
	}

	@Override
	protected List<String> toStringList(final CalendarEvent row) {
		log.debug("converting CalendarEvent row {} to string list.", row);
		final DateFormat dateTimeFormat = new SimpleDateFormat("dd MMM yyyy HH:mm");
		final String statusText = translateStatus(row.getStatus());
		return Arrays.asList(row.getEventDateTime() != null ? dateTimeFormat.format(row.getEventDateTime()) : "",
				"<a href='/admin/calendario/" + row.getId() + "'>" + row.getTitle() + "</a>", statusText,
				row.getDurationMinutes() != null ? row.getDurationMinutes() + " min" : "",
				"<a href='#' class='btn action-btn btn-danger btn-sm delete-btn' data-id='" + row.getId()
						+ "'><i class='fas fa-trash fa-sm fa-fw'></i> </a>");
	}

	@Override
	protected List<CalendarEvent> getData() {
		log.debug("getting CalendarEvent records for paciente.");
		// This method is not used when we override getRows
		return calendarEventService.findAll();
	}

	protected Page<CalendarEvent> getRows(final PagingRequest pagingRequest, @NonNull final Long pacienteId) {
		log.debug("getting CalendarEvent rows filtered by pacienteId: {}", pacienteId);
		final List<CalendarEvent> pacienteEvents = calendarEventService.findByPacienteId(pacienteId);
		return getPage(pagingRequest, pacienteEvents);
	}

	@Override
	protected Page<CalendarEvent> getRows(final PagingRequest pagingRequest) {
		log.debug("getting CalendarEvent rows (should not be called, use getRows with pacienteId)");
		return getPage(pagingRequest, calendarEventService.findAll());
	}

	@Override
	protected Comparator<CalendarEvent> getComparator(final String column, final Direction dir) {
		log.debug("getting CalendarEvent comparator with column {} and direction {}.", column, dir);
		final Comparator<CalendarEvent> comparator;
		switch (column) {
			case "title":
			case "titulo":
				comparator = Comparator.comparing(CalendarEvent::getTitle,
						Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
				break;
			case "eventDateTime":
			case "fecha":
				comparator = Comparator.comparing(CalendarEvent::getEventDateTime,
						Comparator.nullsLast(Date::compareTo));
				break;
			case "duration":
			case "duracion":
				comparator = Comparator.comparing(CalendarEvent::getDurationMinutes,
						Comparator.nullsLast(Integer::compareTo));
				break;
			case "status":
			case "estado":
				comparator = Comparator.comparing(CalendarEvent::getStatus, Comparator.nullsLast(Enum::compareTo));
				break;
			default:
				comparator = Comparator.comparing(CalendarEvent::getEventDateTime,
						Comparator.nullsLast(Date::compareTo));
		}
		return dir == Direction.desc ? comparator.reversed() : comparator;
	}

	@Override
	protected List<Column> getColumns() {
		log.debug("getting CalendarEvent columns.");
		return Stream.of("fecha", "titulo", "estado", "duracion", "actions")
			.map(Column::new)
			.collect(Collectors.toList());
	}

	@Override
	protected Predicate<CalendarEvent> getPredicate(final String value) {
		log.debug("getting CalendarEvent predicate with value {}.", value);
		final String lowerValue = value.toLowerCase();
		return row -> (row.getTitle() != null && row.getTitle().toLowerCase().contains(lowerValue))
				|| (row.getDescription() != null && row.getDescription().toLowerCase().contains(lowerValue))
				|| (row.getEventDateTime() != null
						&& new SimpleDateFormat("dd MMM yyyy HH:mm").format(row.getEventDateTime())
							.toLowerCase()
							.contains(lowerValue))
				|| (row.getStatus() != null && translateStatus(row.getStatus()).toLowerCase().contains(lowerValue));
	}

	private String translateStatus(final EventStatus status) {
		if (status == null) {
			return "";
		}
		return switch (status) {
			case SCHEDULED -> "Agendado";
			case COMPLETED -> "Completado";
			case CANCELLED -> "Cancelado";
		};
	}

	@org.springframework.web.bind.annotation.GetMapping("charts/{chartType}")
	public com.nutriconsultas.charts.ChartResponse getChartData(@PathVariable @NonNull final Long id,
			@PathVariable @NonNull final String chartType) {
		log.debug("Getting chart data for paciente {} with chart type {}", id, chartType);
		final List<CalendarEvent> consultas = calendarEventService.findByPacienteId(id);
		// Sort by date ascending
		consultas.sort(Comparator.comparing(CalendarEvent::getEventDateTime, Comparator.nullsLast(Date::compareTo)));

		final java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy");
		final List<String> labels = consultas.stream()
			.filter(c -> c.getEventDateTime() != null)
			.map(c -> dateFormat.format(c.getEventDateTime()))
			.collect(Collectors.toList());

		final java.util.Map<String, Object> data = new java.util.HashMap<>();

		// Add all the data fields needed for the histogram
		final List<Double> pesoData = consultas.stream().map(CalendarEvent::getPeso).collect(Collectors.toList());
		data.put("peso", pesoData);

		final List<Double> estaturaData = consultas.stream()
			.map(CalendarEvent::getEstatura)
			.collect(Collectors.toList());
		data.put("estatura", estaturaData);

		final List<Double> imcData = consultas.stream().map(CalendarEvent::getImc).collect(Collectors.toList());
		data.put("imc", imcData);

		final List<Double> grasaCorporalData = consultas.stream()
			.map(CalendarEvent::getIndiceGrasaCorporal)
			.collect(Collectors.toList());
		data.put("grasaCorporal", grasaCorporalData);

		// Presión arterial - combinamos sistólica y diastólica
		final List<String> presionArterialData = consultas.stream().map(c -> {
			if (c.getSistolica() != null && c.getDiastolica() != null) {
				return c.getSistolica() + "/" + c.getDiastolica();
			}
			return null;
		}).collect(Collectors.toList());
		data.put("presionArterial", presionArterialData);

		// También guardamos sistólica y diastólica por separado para gráficos
		final List<Integer> sistolicaData = consultas.stream()
			.map(CalendarEvent::getSistolica)
			.collect(Collectors.toList());
		data.put("sistolica", sistolicaData);

		final List<Integer> diastolicaData = consultas.stream()
			.map(CalendarEvent::getDiastolica)
			.collect(Collectors.toList());
		data.put("diastolica", diastolicaData);

		final List<Integer> indiceGlucemicoData = consultas.stream()
			.map(CalendarEvent::getIndiceGlucemico)
			.collect(Collectors.toList());
		data.put("indiceGlucemico", indiceGlucemicoData);

		return new com.nutriconsultas.charts.ChartResponse(labels, data);
	}

}
