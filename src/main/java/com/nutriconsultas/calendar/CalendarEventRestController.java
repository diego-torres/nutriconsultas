package com.nutriconsultas.calendar;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.controller.AbstractGridController;
import com.nutriconsultas.dataTables.paging.Column;
import com.nutriconsultas.dataTables.paging.Direction;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/calendario")
@Slf4j
public class CalendarEventRestController extends AbstractGridController<CalendarEvent> {

	@Autowired
	private CalendarEventService service;

	@Override
	protected List<String> toStringList(final CalendarEvent row) {
		log.debug("converting CalendarEvent row {} to string list.", row);
		final DateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		return Arrays.asList("<a href='/admin/calendario/" + row.getId() + "'>" + row.getTitle() + "</a>",
				row.getEventDateTime() != null ? dateTimeFormat.format(row.getEventDateTime()) : "",
				row.getPaciente() != null ? row.getPaciente().getName() : "",
				row.getDurationMinutes() != null ? row.getDurationMinutes() + " min" : "",
				row.getStatus() != null ? row.getStatus().name() : "");
	}

	@Override
	protected List<CalendarEvent> getData() {
		log.debug("getting all CalendarEvent records.");
		return service.findAll();
	}

	@Override
	protected Predicate<CalendarEvent> getPredicate(final String value) {
		final String lowerValue = value.toLowerCase();
		return row -> (row.getTitle() != null && row.getTitle().toLowerCase().contains(lowerValue))
				|| (row.getDescription() != null && row.getDescription().toLowerCase().contains(lowerValue))
				|| (row.getPaciente() != null && row.getPaciente().getName().toLowerCase().contains(lowerValue));
	}

	@Override
	protected Comparator<CalendarEvent> getComparator(final String column, final Direction dir) {
		log.debug("getting CalendarEvent comparator with column {} and direction {}.", column, dir);
		final Comparator<CalendarEvent> comparator;
		switch (column) {
			case "title":
				comparator = Comparator.comparing(CalendarEvent::getTitle,
						Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
				break;
			case "eventDateTime":
				comparator = Comparator.comparing(CalendarEvent::getEventDateTime,
						Comparator.nullsLast(Date::compareTo));
				break;
			case "paciente":
				comparator = Comparator.comparing(e -> e.getPaciente() != null ? e.getPaciente().getName() : "",
						Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
				break;
			case "duration":
				comparator = Comparator.comparing(CalendarEvent::getDurationMinutes,
						Comparator.nullsLast(Integer::compareTo));
				break;
			case "status":
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
		return Stream.of("title", "eventDateTime", "paciente", "duration", "status")
			.map(Column::new)
			.collect(Collectors.toList());
	}

}
