package com.nutriconsultas.calendar;

import java.util.Date;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.nutriconsultas.controller.AbstractAuthorizedController;
import com.nutriconsultas.paciente.PacienteRepository;

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

	@GetMapping(path = "/admin/calendario/nuevo")
	public String nuevo(final Model model) {
		log.debug("Starting nuevo method");
		model.addAttribute("activeMenu", "calendario");
		final CalendarEvent event = new CalendarEvent();
		event.setEventDateTime(new Date());
		event.setDurationMinutes(60);
		event.setStatus(EventStatus.SCHEDULED);
		model.addAttribute("event", event);
		model.addAttribute("pacientes", pacienteRepository.findAll());
		model.addAttribute("statuses", EventStatus.values());
		log.debug("Finished nuevo method with model {}", model);
		return "sbadmin/calendar/formulario";
	}

	@PostMapping(path = "/admin/calendario/nuevo")
	public String addEvent(@Valid final CalendarEvent event, final BindingResult result, final Model model,
			@org.springframework.web.bind.annotation.RequestParam(required = false) final Long pacienteId) {
		log.debug("Grabando nuevo evento: " + event.getTitle());
		String resultView;
		if (result.hasErrors()) {
			model.addAttribute("activeMenu", "calendario");
			model.addAttribute("pacientes", pacienteRepository.findAll());
			model.addAttribute("statuses", EventStatus.values());
			resultView = "sbadmin/calendar/formulario";
		}
		else {
			if (pacienteId != null) {
				final com.nutriconsultas.paciente.Paciente paciente = pacienteRepository.findById(pacienteId)
					.orElseThrow(
							() -> new IllegalArgumentException("No se ha encontrado paciente con id " + pacienteId));
				event.setPaciente(paciente);
			}
			calendarEventService.save(event);
			resultView = "redirect:/admin/calendario";
		}
		return resultView;
	}

	@GetMapping(path = "/admin/calendario/{id}")
	public String verEvento(@PathVariable @NonNull final Long id, final Model model) {
		log.debug("Cargando evento {}", id);
		final CalendarEvent event = calendarEventService.findById(id);
		if (event == null) {
			throw new IllegalArgumentException("No se ha encontrado evento con id " + id);
		}

		model.addAttribute("activeMenu", "calendario");
		model.addAttribute("event", event);
		return "sbadmin/calendar/ver";
	}

	@GetMapping(path = "/admin/calendario/{id}/editar")
	public String editarEvento(@PathVariable @NonNull final Long id, final Model model) {
		log.debug("Cargando evento para editar {}", id);
		final CalendarEvent event = calendarEventService.findById(id);
		if (event == null) {
			throw new IllegalArgumentException("No se ha encontrado evento con id " + id);
		}

		model.addAttribute("activeMenu", "calendario");
		model.addAttribute("event", event);
		model.addAttribute("pacientes", pacienteRepository.findAll());
		model.addAttribute("statuses", EventStatus.values());
		return "sbadmin/calendar/formulario";
	}

	@PostMapping(path = "/admin/calendario/{id}/editar")
	public String updateEvent(@PathVariable @NonNull final Long id, @Valid final CalendarEvent event,
			final BindingResult result, final Model model,
			@org.springframework.web.bind.annotation.RequestParam(required = false) final Long pacienteId) {
		log.debug("Actualizando evento {}", id);
		String resultView;
		if (result.hasErrors()) {
			model.addAttribute("activeMenu", "calendario");
			model.addAttribute("pacientes", pacienteRepository.findAll());
			model.addAttribute("statuses", EventStatus.values());
			resultView = "sbadmin/calendar/formulario";
		}
		else {
			final CalendarEvent existingEvent = calendarEventService.findById(id);
			if (existingEvent == null) {
				throw new IllegalArgumentException("No se ha encontrado evento con id " + id);
			}
			event.setId(id);
			if (pacienteId != null) {
				final com.nutriconsultas.paciente.Paciente paciente = pacienteRepository.findById(pacienteId)
					.orElseThrow(
							() -> new IllegalArgumentException("No se ha encontrado paciente con id " + pacienteId));
				event.setPaciente(paciente);
			}
			calendarEventService.save(event);
			resultView = "redirect:/admin/calendario";
		}
		return resultView;
	}

	@PostMapping(path = "/admin/calendario/{id}/eliminar")
	public String deleteEvent(@PathVariable @NonNull final Long id) {
		log.debug("Eliminando evento {}", id);
		calendarEventService.delete(id);
		return "redirect:/admin/calendario";
	}

}
