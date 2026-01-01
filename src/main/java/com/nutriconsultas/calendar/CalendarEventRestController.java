package com.nutriconsultas.calendar;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nutriconsultas.controller.AbstractGridController;
import com.nutriconsultas.dataTables.paging.Column;
import com.nutriconsultas.dataTables.paging.Direction;
import com.nutriconsultas.paciente.BodyFatCalculatorService;
import com.nutriconsultas.paciente.NivelPeso;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/rest/calendario")
@Slf4j
public class CalendarEventRestController extends AbstractGridController<CalendarEvent> {

	@Autowired
	private CalendarEventService service;

	@Autowired
	private PacienteRepository pacienteRepository;

	@Autowired
	private BodyFatCalculatorService bodyFatCalculatorService;

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

	@GetMapping("/events")
	public List<Map<String, Object>> getCalendarEvents(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Date start,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Date end) {
		log.debug("Getting calendar events from {} to {}", start, end);
		final List<CalendarEvent> events;
		if (start != null && end != null) {
			events = service.findEventsBetweenDates(start, end);
		}
		else {
			events = service.findAll();
		}
		return events.stream().map(this::toCalendarEventMap).collect(Collectors.toList());
	}

	private Map<String, Object> toCalendarEventMap(final CalendarEvent event) {
		final Map<String, Object> eventMap = new HashMap<>();
		eventMap.put("id", event.getId().toString());
		eventMap.put("title", event.getTitle());
		if (event.getEventDateTime() != null) {
			eventMap.put("start", formatDateForCalendar(event.getEventDateTime()));
			if (event.getDurationMinutes() != null && event.getDurationMinutes() > 0) {
				final LocalDateTime eventDateTime = event.getEventDateTime()
					.toInstant()
					.atZone(ZoneId.systemDefault())
					.toLocalDateTime();
				final LocalDateTime endDateTime = eventDateTime.plusMinutes(event.getDurationMinutes());
				final Date endDate = Date.from(endDateTime.atZone(ZoneId.systemDefault()).toInstant());
				eventMap.put("end", formatDateForCalendar(endDate));
			}
		}
		eventMap.put("allDay", false);
		// Set event color based on status
		if (event.getStatus() != null) {
			eventMap.put("backgroundColor", getEventColor(event.getStatus()));
			eventMap.put("borderColor", getEventColor(event.getStatus()));
		}
		final Map<String, Object> extendedProps = new HashMap<>();
		if (event.getPaciente() != null) {
			extendedProps.put("paciente", event.getPaciente().getName());
			extendedProps.put("pacienteId", event.getPaciente().getId());
		}
		if (event.getStatus() != null) {
			extendedProps.put("status", event.getStatus().name());
		}
		if (event.getDescription() != null) {
			extendedProps.put("description", event.getDescription());
		}
		if (event.getSummaryNotes() != null) {
			extendedProps.put("summaryNotes", event.getSummaryNotes());
		}
		extendedProps.put("durationMinutes", event.getDurationMinutes());
		if (event.getPeso() != null) {
			extendedProps.put("peso", event.getPeso());
		}
		if (event.getEstatura() != null) {
			extendedProps.put("estatura", event.getEstatura());
		}
		if (event.getImc() != null) {
			extendedProps.put("imc", event.getImc());
		}
		if (event.getIndiceGrasaCorporal() != null) {
			extendedProps.put("indiceGrasaCorporal", event.getIndiceGrasaCorporal());
		}
		if (event.getNivelPeso() != null) {
			extendedProps.put("nivelPeso", event.getNivelPeso().name());
		}
		if (event.getSistolica() != null) {
			extendedProps.put("sistolica", event.getSistolica());
		}
		if (event.getDiastolica() != null) {
			extendedProps.put("diastolica", event.getDiastolica());
		}
		if (event.getPulso() != null) {
			extendedProps.put("pulso", event.getPulso());
		}
		if (event.getIndiceGlucemico() != null) {
			extendedProps.put("indiceGlucemico", event.getIndiceGlucemico());
		}
		if (event.getSpo2() != null) {
			extendedProps.put("spo2", event.getSpo2());
		}
		if (event.getTemperatura() != null) {
			extendedProps.put("temperatura", event.getTemperatura());
		}
		eventMap.put("extendedProps", extendedProps);
		eventMap.put("url", "/admin/calendario/" + event.getId());
		return eventMap;
	}

	private String getEventColor(final EventStatus status) {
		return switch (status) {
			case SCHEDULED -> "#4e73df"; // Primary blue
			case COMPLETED -> "#1cc88a"; // Success green
			case CANCELLED -> "#e74a3b"; // Danger red
		};
	}

	private String formatDateForCalendar(final Date date) {
		if (date == null) {
			return null;
		}
		final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		return dateFormat.format(date);
	}

	@GetMapping("/pacientes")
	public List<Map<String, Object>> getPacientes() {
		log.debug("Getting all pacientes for appointment creation");
		final List<Paciente> pacientes = pacienteRepository.findAll();
		return pacientes.stream().map(p -> {
			final Map<String, Object> pacienteMap = new HashMap<>();
			pacienteMap.put("id", p.getId());
			pacienteMap.put("name", p.getName());
			return pacienteMap;
		}).collect(Collectors.toList());
	}

	@GetMapping("/next-available-time")
	public Map<String, Object> getNextAvailableTime(@RequestParam final String date) {
		log.debug("Finding next available time for date: {}", date);
		LocalDate parsedLocalDate;
		try {
			// Parse date string (format: YYYY-MM-DD)
			parsedLocalDate = LocalDate.parse(date);
		}
		catch (final java.time.format.DateTimeParseException e) {
			log.error("Error parsing date: {}", date, e);
			final Map<String, Object> errorResult = new HashMap<>();
			errorResult.put("available", false);
			errorResult.put("error", "Invalid date format");
			return errorResult;
		}

		LocalDateTime candidateTime = parsedLocalDate.atTime(8, 0);

		// Check if the requested date is in the past (before today)
		// For past dates, do not attempt to calculate available time
		// This allows saving unplanned historical consultation records
		final LocalDate today = LocalDate.now();
		final LocalDate requestedDate = parsedLocalDate;

		// If the requested date is in the past, return the date with default time
		// without calculating availability (for historical records)
		// No availability calculation is performed for past dates
		if (requestedDate.isBefore(today)) {
			log.debug("Date {} is in the past, returning default time without availability check", date);
			final Map<String, Object> result = new HashMap<>();
			result.put("available", true);
			result.put("dateTime",
					formatDateForCalendar(Date.from(candidateTime.atZone(ZoneId.systemDefault()).toInstant())));
			result.put("isPastDate", true);
			return result; // Exit early - do not attempt to calculate available time
		}
		final LocalDateTime endOfDay = parsedLocalDate.atTime(17, 0);

		// Get all events for this date
		final LocalDateTime startOfDay = parsedLocalDate.atStartOfDay();
		final LocalDateTime nextDay = parsedLocalDate.plusDays(1).atStartOfDay();

		final Date startDate = Date.from(startOfDay.atZone(ZoneId.systemDefault()).toInstant());
		final Date endDate = Date.from(nextDay.atZone(ZoneId.systemDefault()).toInstant());
		final List<CalendarEvent> events = service.findEventsBetweenDates(startDate != null ? startDate : new Date(0),
				endDate != null ? endDate : new Date(Long.MAX_VALUE));

		// If the requested date is today, start from current time if it's after 8 AM
		final LocalDateTime nowWithTime = LocalDateTime.now();
		if (requestedDate.equals(today)) {
			// It's today, check if current time is after 8 AM
			if (nowWithTime.toLocalTime().isAfter(LocalTime.of(8, 0))) {
				// Current time is after 8 AM, start from current time
				candidateTime = nowWithTime.truncatedTo(ChronoUnit.SECONDS);
				// Round up to next hour
				if (candidateTime.getMinute() > 0) {
					candidateTime = candidateTime.plusHours(1).withMinute(0).withSecond(0);
				}
			}
			// Otherwise, candidateTime is already set to 8:00 AM, which is fine
		}

		// Find next available hour
		while (!candidateTime.isAfter(endOfDay)) {
			final LocalDateTime candidateEnd = candidateTime.plusHours(1);

			// Check if this hour is available (no overlapping events)
			boolean isAvailable = true;
			for (final CalendarEvent event : events) {
				if (event.getEventDateTime() == null) {
					continue;
				}
				final LocalDateTime eventStart = event.getEventDateTime()
					.toInstant()
					.atZone(ZoneId.systemDefault())
					.toLocalDateTime();
				final int durationMinutes = event.getDurationMinutes() != null ? event.getDurationMinutes() : 60;
				final LocalDateTime eventEnd = eventStart.plusMinutes(durationMinutes);

				// Check for overlap
				if (candidateTime.isBefore(eventEnd) && candidateEnd.isAfter(eventStart)) {
					isAvailable = false;
					break;
				}
			}

			if (isAvailable) {
				final Map<String, Object> result = new HashMap<>();
				result.put("available", true);
				result.put("dateTime",
						formatDateForCalendar(Date.from(candidateTime.atZone(ZoneId.systemDefault()).toInstant())));
				return result;
			}

			candidateTime = candidateTime.plusHours(1);
		}

		// No available time found
		final Map<String, Object> result = new HashMap<>();
		result.put("available", false);
		return result;
	}

	@PostMapping("/events")
	public ResponseEntity<Map<String, Object>> saveEvent(@RequestBody final Map<String, Object> eventData) {
		log.debug("Saving new calendar event: {}", eventData);
		try {
			final CalendarEvent event = createEventFromData(eventData);
			setBiochemicalFields(event, eventData);
			final CalendarEvent savedEvent = service.save(event);
			final Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("event", toCalendarEventMap(savedEvent));
			return ResponseEntity.ok(response);
		}
		catch (final Exception e) {
			log.error("Error saving calendar event", e);
			final Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("success", false);
			errorResponse.put("error", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
		}
	}

	@NonNull
	private CalendarEvent createEventFromData(final Map<String, Object> eventData) {
		final CalendarEvent event = new CalendarEvent();
		event.setTitle((String) eventData.get("title"));
		if (eventData.get("description") != null) {
			event.setDescription((String) eventData.get("description"));
		}
		if (eventData.get("durationMinutes") != null) {
			event.setDurationMinutes(Integer.parseInt(eventData.get("durationMinutes").toString()));
		}
		else {
			event.setDurationMinutes(60);
		}
		if (eventData.get("status") != null) {
			event.setStatus(EventStatus.valueOf((String) eventData.get("status")));
		}
		else {
			event.setStatus(EventStatus.SCHEDULED);
		}
		if (eventData.get("eventDateTime") != null) {
			final Date parsedDate = parseEventDateTime((String) eventData.get("eventDateTime"));
			if (parsedDate == null) {
				throw new IllegalArgumentException("Invalid date format: " + eventData.get("eventDateTime"));
			}
			event.setEventDateTime(parsedDate);
		}
		if (eventData.get("pacienteId") != null) {
			final Long pacienteId = Long.parseLong(eventData.get("pacienteId").toString());
			final Paciente paciente = pacienteRepository.findById(pacienteId)
				.orElseThrow(
						() -> new IllegalArgumentException("No se ha encontrado paciente con id " + pacienteId));
			event.setPaciente(paciente);
		}
		return event;
	}

	private Date parseEventDateTime(final String dateTimeStr) {
		final String[] formats = { "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm", "yyyy-MM-dd HH:mm:ss",
				"yyyy-MM-dd HH:mm" };
		for (final String format : formats) {
			try {
				final DateFormat dateFormat = new SimpleDateFormat(format);
				return dateFormat.parse(dateTimeStr);
			}
			catch (final java.text.ParseException e) {
				// Try next format
				continue;
			}
		}
		log.error("Error parsing eventDateTime: {}", dateTimeStr);
		return null;
	}

	private void setBiochemicalFields(final CalendarEvent event, final Map<String, Object> eventData) {
		// Vital signs and basic measurements
		setDoubleField(eventData, "peso", event::setPeso);
		setDoubleField(eventData, "estatura", event::setEstatura);
		setDoubleField(eventData, "imc", event::setImc);
		setDoubleField(eventData, "indiceGrasaCorporal", event::setIndiceGrasaCorporal);
		setIntegerField(eventData, "sistolica", event::setSistolica);
		setIntegerField(eventData, "diastolica", event::setDiastolica);
		setIntegerField(eventData, "pulso", event::setPulso);
		setIntegerField(eventData, "indiceGlucemico", event::setIndiceGlucemico);
		setDoubleField(eventData, "spo2", event::setSpo2);
		setDoubleField(eventData, "temperatura", event::setTemperatura);
		if (eventData.get("nivelPeso") != null) {
			event.setNivelPeso(NivelPeso.valueOf((String) eventData.get("nivelPeso")));
		}
		// Lipid profile
		setDoubleField(eventData, "hdl", event::setHdl);
		setDoubleField(eventData, "ldl", event::setLdl);
		setDoubleField(eventData, "trigliceridos", event::setTrigliceridos);
		setDoubleField(eventData, "colesterolTotal", event::setColesterolTotal);
		// Blood chemistry
		setDoubleField(eventData, "glucosa", event::setGlucosa);
		setDoubleField(eventData, "hba1c", event::setHba1c);
		setDoubleField(eventData, "creatinina", event::setCreatinina);
		setDoubleField(eventData, "urea", event::setUrea);
		setDoubleField(eventData, "bun", event::setBun);
		// Liver function
		setDoubleField(eventData, "alt", event::setAlt);
		setDoubleField(eventData, "ast", event::setAst);
		setDoubleField(eventData, "bilirrubina", event::setBilirrubina);
		// Complete blood count
		setDoubleField(eventData, "hemoglobina", event::setHemoglobina);
		setDoubleField(eventData, "hematocrito", event::setHematocrito);
		setDoubleField(eventData, "leucocitos", event::setLeucocitos);
		setDoubleField(eventData, "plaquetas", event::setPlaquetas);
		// Other tests
		setDoubleField(eventData, "vitaminaD", event::setVitaminaD);
		setDoubleField(eventData, "vitaminaB12", event::setVitaminaB12);
		setDoubleField(eventData, "hierro", event::setHierro);
		setDoubleField(eventData, "ferritina", event::setFerritina);
	}

	private void setDoubleField(final Map<String, Object> eventData, final String fieldName,
			final java.util.function.Consumer<Double> setter) {
		if (eventData.get(fieldName) != null) {
			setter.accept(Double.parseDouble(eventData.get(fieldName).toString()));
		}
	}

	private void setIntegerField(final Map<String, Object> eventData, final String fieldName,
			final java.util.function.Consumer<Integer> setter) {
		if (eventData.get(fieldName) != null) {
			setter.accept(Integer.parseInt(eventData.get(fieldName).toString()));
		}
	}

	@GetMapping("/events/{id}")
	public ResponseEntity<Map<String, Object>> getEvent(@PathVariable @NonNull final Long id) {
		log.debug("Getting calendar event with id: {}", id);
		try {
			final CalendarEvent event = service.findById(id);
			if (event == null) {
				final Map<String, Object> errorResponse = new HashMap<>();
				errorResponse.put("success", false);
				errorResponse.put("error", "Event not found");
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
			}
			final Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("event", toCalendarEventMap(event));
			return ResponseEntity.ok(response);
		}
		catch (final Exception e) {
			log.error("Error getting calendar event", e);
			final Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("success", false);
			errorResponse.put("error", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
		}
	}

	@PostMapping("/events/{id}")
	public ResponseEntity<Map<String, Object>> updateEvent(@PathVariable @NonNull final Long id,
			@RequestBody final Map<String, Object> eventData) {
		log.debug("Updating calendar event {}: {}", id, eventData);
		try {
			final CalendarEvent existingEvent = service.findById(id);
			if (existingEvent == null) {
				final Map<String, Object> errorResponse = new HashMap<>();
				errorResponse.put("success", false);
				errorResponse.put("error", "Event not found");
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
			}
			if (eventData.get("status") != null) {
				existingEvent.setStatus(EventStatus.valueOf((String) eventData.get("status")));
			}
			if (eventData.get("summaryNotes") != null) {
				existingEvent.setSummaryNotes((String) eventData.get("summaryNotes"));
			}
			// Handle peso and estatura, and calculate IMC and body fat if needed
			Double peso = null;
			Double estatura = null;
			if (eventData.get("peso") != null) {
				peso = Double.parseDouble(eventData.get("peso").toString());
				existingEvent.setPeso(peso);
			}
			if (eventData.get("estatura") != null) {
				estatura = Double.parseDouble(eventData.get("estatura").toString());
				existingEvent.setEstatura(estatura);
			}

			// Calculate IMC if peso and estatura are provided
			if (peso != null && estatura != null && estatura > 0) {
				final Double imc = peso / Math.pow(estatura, 2);
				final NivelPeso nivelPeso = imc > 30.0d ? NivelPeso.SOBREPESO
						: imc > 25.0d ? NivelPeso.ALTO : imc > 18.5d ? NivelPeso.NORMAL : NivelPeso.BAJO;
				existingEvent.setImc(imc);
				existingEvent.setNivelPeso(nivelPeso);

				// Calculate body fat percentage if patient data is available
				final Paciente paciente = existingEvent.getPaciente();
				if (paciente != null && paciente.getDob() != null && paciente.getGender() != null) {
					final Integer age = calculateAge(paciente.getDob());
					if (age != null) {
						final Double bodyFatPercentage = bodyFatCalculatorService.calculateBodyFatPercentage(imc, age,
								paciente.getGender());
						existingEvent.setIndiceGrasaCorporal(bodyFatPercentage);
					}
				}
			}
			else {
				// If IMC is explicitly provided, use it
				if (eventData.get("imc") != null) {
					existingEvent.setImc(Double.parseDouble(eventData.get("imc").toString()));
				}
				// If indiceGrasaCorporal is explicitly provided, use it
				if (eventData.get("indiceGrasaCorporal") != null) {
					existingEvent
						.setIndiceGrasaCorporal(Double.parseDouble(eventData.get("indiceGrasaCorporal").toString()));
				}
			}
			if (eventData.get("nivelPeso") != null) {
				existingEvent
					.setNivelPeso(com.nutriconsultas.paciente.NivelPeso.valueOf((String) eventData.get("nivelPeso")));
			}
			if (eventData.get("sistolica") != null) {
				existingEvent.setSistolica(Integer.parseInt(eventData.get("sistolica").toString()));
			}
			if (eventData.get("diastolica") != null) {
				existingEvent.setDiastolica(Integer.parseInt(eventData.get("diastolica").toString()));
			}
			if (eventData.get("pulso") != null) {
				existingEvent.setPulso(Integer.parseInt(eventData.get("pulso").toString()));
			}
			if (eventData.get("indiceGlucemico") != null) {
				existingEvent.setIndiceGlucemico(Integer.parseInt(eventData.get("indiceGlucemico").toString()));
			}
			if (eventData.get("spo2") != null) {
				existingEvent.setSpo2(Double.parseDouble(eventData.get("spo2").toString()));
			}
			if (eventData.get("temperatura") != null) {
				existingEvent.setTemperatura(Double.parseDouble(eventData.get("temperatura").toString()));
			}
			final CalendarEvent savedEvent = service.save(existingEvent);
			final Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("event", toCalendarEventMap(savedEvent));
			return ResponseEntity.ok(response);
		}
		catch (final Exception e) {
			log.error("Error updating calendar event", e);
			final Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("success", false);
			errorResponse.put("error", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
		}
	}

	/**
	 * Calculates age from date of birth.
	 * @param dob Date of birth (can be java.util.Date or java.sql.Date)
	 * @return Age in years, or null if dob is null or in the future
	 */
	private Integer calculateAge(final Date dob) {
		if (dob == null) {
			return null;
		}
		try {
			// Handle both java.util.Date and java.sql.Date
			// java.sql.Date doesn't support toInstant(), so we convert to java.util.Date
			// first
			final java.util.Date utilDate = dob instanceof java.sql.Date ? new java.util.Date(dob.getTime())
					: (java.util.Date) dob;
			final LocalDate birthDate = utilDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			final LocalDate currentDate = LocalDate.now();
			if (birthDate.isAfter(currentDate)) {
				log.warn("Date of birth is in the future: {}", dob);
				return null;
			}
			return currentDate.getYear() - birthDate.getYear()
					- (currentDate.getDayOfYear() < birthDate.getDayOfYear() ? 1 : 0);
		}
		catch (final Exception e) {
			log.warn("Error calculating age from date of birth: {}", dob, e);
			return null;
		}
	}

}
