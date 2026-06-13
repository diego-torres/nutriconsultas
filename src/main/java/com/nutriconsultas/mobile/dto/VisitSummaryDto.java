package com.nutriconsultas.mobile.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.calendar.EventStatus;

/**
 * Lightweight consultation summary for {@code GET /rest/mobile/patient/visits} (#91).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VisitSummaryDto(Long id, Instant eventDateTime, String title, EventStatus status, Integer durationMinutes,
		String summaryNotes) {

	public static VisitSummaryDto fromEntity(final CalendarEvent event) {
		if (event == null) {
			return null;
		}
		final Instant eventInstant = event.getEventDateTime() != null ? event.getEventDateTime().toInstant() : null;
		return new VisitSummaryDto(event.getId(), eventInstant, event.getTitle(), event.getStatus(),
				event.getDurationMinutes(), event.getSummaryNotes());
	}

}
