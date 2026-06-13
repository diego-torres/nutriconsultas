package com.nutriconsultas.mobile.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.calendar.EventStatus;
import com.nutriconsultas.paciente.NivelPeso;

/**
 * Full consultation detail for {@code GET /rest/mobile/patient/visits/{visitId}} (#92).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VisitDetailDto(Long id, Instant eventDateTime, String title, EventStatus status, Integer durationMinutes,
		String summaryNotes, String description, Double peso, Double estatura, Double imc, Double indiceGrasaCorporal,
		NivelPeso nivelPeso, Integer sistolica, Integer diastolica, Integer pulso, Integer indiceGlucemico, Double spo2,
		Double temperatura) {

	public static VisitDetailDto fromEntity(final CalendarEvent event) {
		if (event == null) {
			return null;
		}
		final Instant eventInstant = event.getEventDateTime() != null ? event.getEventDateTime().toInstant() : null;
		return new VisitDetailDto(event.getId(), eventInstant, event.getTitle(), event.getStatus(),
				event.getDurationMinutes(), event.getSummaryNotes(), event.getDescription(), event.getPeso(),
				event.getEstatura(), event.getImc(), event.getIndiceGrasaCorporal(), event.getNivelPeso(),
				event.getSistolica(), event.getDiastolica(), event.getPulso(), event.getIndiceGlucemico(),
				event.getSpo2(), event.getTemperatura());
	}

}
