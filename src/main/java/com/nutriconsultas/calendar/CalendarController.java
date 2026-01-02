package com.nutriconsultas.calendar;

import java.util.Date;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.controller.AbstractAuthorizedController;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.util.LogRedaction;

@Controller
@Slf4j
public class CalendarController extends AbstractAuthorizedController {

	@Autowired
	private CalendarEventService calendarEventService;

	@Autowired
	private PacienteRepository pacienteRepository;

	@GetMapping(path = "/admin/calendario")
	public String listado(final Model model) {
		log.debug("Listado de eventos del calendario");
		model.addAttribute("activeMenu", "calendario");
		return "sbadmin/calendar/listado";
	}

	/**
	 * Gets the user ID from the OAuth2 principal.
	 * @param principal the OAuth2 principal
	 * @return the user ID (sub claim) or null if not available
	 */
	private String getUserId(@AuthenticationPrincipal final OidcUser principal) {
		if (principal == null) {
			log.warn("OAuth2 principal is null, cannot get user ID");
			return null;
		}
		final String userId = principal.getSubject();
		log.debug("Retrieved user ID: {}", userId);
		return userId;
	}

	@GetMapping(path = "/admin/calendario/nuevo")
	public String nuevo(final Model model, @AuthenticationPrincipal final OidcUser principal) {
		log.debug("Starting nuevo method");
		final String userId = getUserId(principal);
		if (userId == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No se pudo identificar al usuario");
		}
		model.addAttribute("activeMenu", "calendario");
		final CalendarEvent event = new CalendarEvent();
		event.setEventDateTime(new Date());
		event.setDurationMinutes(60);
		event.setStatus(EventStatus.SCHEDULED);
		model.addAttribute("event", event);
		model.addAttribute("pacientes", pacienteRepository.findByUserId(userId));
		model.addAttribute("statuses", EventStatus.values());
		log.debug("Finished nuevo method with model {}", model);
		return "sbadmin/calendar/formulario";
	}

	@PostMapping(path = "/admin/calendario/nuevo")
	public String addEvent(final CalendarEvent event, final BindingResult result, final Model model,
			@org.springframework.web.bind.annotation.RequestParam(required = false) final Long pacienteId,
			@AuthenticationPrincipal final OidcUser principal) {
		log.debug("Grabando nuevo evento: {}", event != null ? event.getTitle() : "null");

		// Check if event is null before using it
		if (event == null) {
			throw new IllegalArgumentException("Event cannot be null");
		}

		final String userId = getUserId(principal);
		if (userId == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No se pudo identificar al usuario");
		}

		// Set paciente from pacienteId parameter before validation
		// This is necessary because paciente is validated as @NotNull but comes from
		// pacienteId parameter
		if (pacienteId != null) {
			final com.nutriconsultas.paciente.Paciente paciente = pacienteRepository
				.findByIdAndUserId(pacienteId, userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"No se ha encontrado paciente con id " + pacienteId));
			event.setPaciente(paciente);
		}

		// Manual validation since paciente comes from parameter, not form binding
		boolean hasErrors = false;
		if (event.getTitle() == null || event.getTitle().isBlank()) {
			result.rejectValue("title", "NotBlank", "El título es requerido");
			hasErrors = true;
		}
		if (event.getEventDateTime() == null) {
			result.rejectValue("eventDateTime", "NotNull", "La fecha y hora son requeridas");
			hasErrors = true;
		}
		if (event.getDurationMinutes() == null) {
			result.rejectValue("durationMinutes", "NotNull", "La duración es requerida");
			hasErrors = true;
		}
		if (event.getStatus() == null) {
			result.rejectValue("status", "NotNull", "El estado es requerido");
			hasErrors = true;
		}
		if (event.getPaciente() == null) {
			result.rejectValue("paciente", "NotNull", "El paciente es requerido");
			hasErrors = true;
		}
		final String resultView;
		if (hasErrors) {
			model.addAttribute("activeMenu", "calendario");
			model.addAttribute("event", event);
			model.addAttribute("pacientes", pacienteRepository.findByUserId(userId));
			model.addAttribute("statuses", EventStatus.values());
			resultView = "sbadmin/calendar/formulario";
		}
		else {
			calendarEventService.save(event);
			resultView = "redirect:/admin/calendario";
		}
		return resultView;
	}

	@GetMapping(path = "/admin/calendario/{id}")
	public String verEvento(@PathVariable @NonNull final Long id, final Model model,
			@AuthenticationPrincipal final OidcUser principal) {
		log.debug("Cargando evento {}", id);
		final String userId = getUserId(principal);
		if (userId == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No se pudo identificar al usuario");
		}
		final CalendarEvent event = calendarEventService.findById(id);
		if (event == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se ha encontrado evento con id " + id);
		}
		// Verify patient ownership
		if (event.getPaciente() == null || !userId.equals(event.getPaciente().getUserId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permiso para acceder a este evento");
		}

		model.addAttribute("activeMenu", "calendario");
		model.addAttribute("event", event);
		return "sbadmin/calendar/ver";
	}

	@GetMapping(path = "/admin/calendario/{id}/editar")
	public String editarEvento(@PathVariable @NonNull final Long id, final Model model,
			@AuthenticationPrincipal final OidcUser principal) {
		log.debug("Cargando evento para editar {}", id);
		final String userId = getUserId(principal);
		if (userId == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No se pudo identificar al usuario");
		}
		final CalendarEvent event = calendarEventService.findById(id);
		if (event == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se ha encontrado evento con id " + id);
		}
		// Verify patient ownership
		if (event.getPaciente() == null || !userId.equals(event.getPaciente().getUserId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permiso para acceder a este evento");
		}

		model.addAttribute("activeMenu", "calendario");
		model.addAttribute("event", event);
		model.addAttribute("pacientes", pacienteRepository.findByUserId(userId));
		model.addAttribute("statuses", EventStatus.values());
		return "sbadmin/calendar/formulario";
	}

	@PostMapping(path = "/admin/calendario/{id}/editar")
	public String updateEvent(@PathVariable @NonNull final Long id, final CalendarEvent event,
			final BindingResult result, final Model model,
			@org.springframework.web.bind.annotation.RequestParam(required = false) final Long pacienteId,
			@AuthenticationPrincipal final OidcUser principal) {
		log.debug("Actualizando evento {}", id);
		log.debug("Event object: {}", event);
		log.debug("PacienteId parameter: {}", pacienteId);

		// Check if event is null before using it
		if (event == null) {
			throw new IllegalArgumentException("Event cannot be null");
		}

		final String userId = getUserId(principal);
		if (userId == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No se pudo identificar al usuario");
		}

		// Verify existing event belongs to user
		final CalendarEvent existingEvent = calendarEventService.findById(id);
		if (existingEvent == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se ha encontrado evento con id " + id);
		}
		if (existingEvent.getPaciente() == null || !userId.equals(existingEvent.getPaciente().getUserId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permiso para acceder a este evento");
		}

		// Set paciente from pacienteId parameter before checking validation errors
		// This is necessary because paciente is validated as @NotNull but comes from
		// pacienteId parameter
		if (pacienteId != null) {
			final com.nutriconsultas.paciente.Paciente paciente = pacienteRepository
				.findByIdAndUserId(pacienteId, userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"No se ha encontrado paciente con id " + pacienteId));
			event.setPaciente(paciente);
			log.debug("Set paciente on event: {}", LogRedaction.redactPaciente(paciente));
		}

		// Check for validation errors after setting paciente
		// Note: We don't use @Valid here because paciente comes from pacienteId
		// parameter,
		// not from form binding, so we handle validation manually
		boolean hasErrors = false;
		if (event.getTitle() == null || event.getTitle().isBlank()) {
			result.rejectValue("title", "NotBlank", "El título es requerido");
			hasErrors = true;
		}
		if (event.getEventDateTime() == null) {
			result.rejectValue("eventDateTime", "NotNull", "La fecha y hora son requeridas");
			hasErrors = true;
		}
		if (event.getDurationMinutes() == null) {
			result.rejectValue("durationMinutes", "NotNull", "La duración es requerida");
			hasErrors = true;
		}
		if (event.getStatus() == null) {
			result.rejectValue("status", "NotNull", "El estado es requerido");
			hasErrors = true;
		}
		if (event.getPaciente() == null) {
			result.rejectValue("paciente", "NotNull", "El paciente es requerido");
			hasErrors = true;
		}

		if (hasErrors) {
			log.error("Validation errors found: {}", result.getAllErrors());
			result.getAllErrors()
				.forEach(error -> log.error("Error: {} - {}", error.getObjectName(), error.getDefaultMessage()));
			model.addAttribute("activeMenu", "calendario");
			model.addAttribute("event", event);
			model.addAttribute("pacientes", pacienteRepository.findByUserId(userId));
			model.addAttribute("statuses", EventStatus.values());
			return "sbadmin/calendar/formulario";
		}
		log.debug("No validation errors, proceeding with update");
		log.debug("Found existing event: {}", LogRedaction.redactCalendarEvent(existingEvent));
		// Update all fields from the form
		existingEvent.setTitle(event.getTitle());
		existingEvent.setDescription(event.getDescription());
		existingEvent.setEventDateTime(event.getEventDateTime());
		existingEvent.setDurationMinutes(event.getDurationMinutes());
		existingEvent.setStatus(event.getStatus());
		// Update paciente if provided, otherwise keep existing
		if (pacienteId != null) {
			log.debug("Updating paciente to id: {}", pacienteId);
			final com.nutriconsultas.paciente.Paciente paciente = pacienteRepository
				.findByIdAndUserId(pacienteId, userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"No se ha encontrado paciente con id " + pacienteId));
			existingEvent.setPaciente(paciente);
		}
		else {
			log.debug("No pacienteId provided, keeping existing paciente");
		}
		// Preserve summaryNotes if it exists (not in form, but should be preserved)
		// summaryNotes is not part of the form, so it will remain unchanged
		log.debug("Saving event: {}", LogRedaction.redactCalendarEvent(existingEvent));
		calendarEventService.save(existingEvent);
		log.debug("Event saved successfully, redirecting to calendar");
		return "redirect:/admin/calendario";
	}

	@PostMapping(path = "/admin/calendario/{id}/eliminar")
	public String deleteEvent(@PathVariable @NonNull final Long id, @AuthenticationPrincipal final OidcUser principal) {
		log.debug("Eliminando evento {}", id);
		final String userId = getUserId(principal);
		if (userId == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No se pudo identificar al usuario");
		}
		final CalendarEvent event = calendarEventService.findById(id);
		if (event == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se ha encontrado evento con id " + id);
		}
		// Verify patient ownership
		if (event.getPaciente() == null || !userId.equals(event.getPaciente().getUserId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permiso para acceder a este evento");
		}
		calendarEventService.delete(id);
		return "redirect:/admin/calendario";
	}

}
