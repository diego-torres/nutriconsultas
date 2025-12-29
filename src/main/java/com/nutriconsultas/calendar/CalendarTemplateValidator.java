package com.nutriconsultas.calendar;

import java.util.Date;
import java.util.Map;

import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.validation.template.BaseTemplateValidator;

/**
 * Validator for calendar templates. Provides mock variables for calendar event pages.
 */
public class CalendarTemplateValidator extends BaseTemplateValidator {

	@Override
	public String getTemplatePathPattern() {
		return "sbadmin/calendar/*";
	}

	@Override
	public Map<String, Object> createMockModelVariables() {
		Map<String, Object> variables = super.createMockModelVariables();

		// Create a new CalendarEvent object with default values
		CalendarEvent event = new CalendarEvent();
		event.setId(0L);
		event.setEventDateTime(new Date());
		event.setTitle("");
		event.setDescription("");
		event.setDurationMinutes(60);
		event.setStatus(EventStatus.SCHEDULED);

		// Create a mock Paciente for the event
		Paciente paciente = new Paciente();
		paciente.setId(0L);
		paciente.setName("");
		event.setPaciente(paciente);

		variables.put("event", event);

		// Mock pacientes list (for form dropdown)
		variables.put("pacientes", new java.util.ArrayList<>());

		// Mock statuses array
		variables.put("statuses", EventStatus.values());

		return variables;
	}

}
