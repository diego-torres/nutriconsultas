package com.nutriconsultas.paciente;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Parses weekday catalog diet ids from assign/edit forms (#525).
 */
public final class PacienteDietaWeekdayRequestSupport {

	private PacienteDietaWeekdayRequestSupport() {
	}

	public static Map<Integer, Long> parseWeekdayCatalogDietaIds(final HttpServletRequest request) {
		final Map<Integer, Long> result = new LinkedHashMap<>();
		if (request == null) {
			return result;
		}
		for (final int day : PacienteDietaWeekdayLabels.ISO_DAYS_MONDAY_FIRST) {
			final String raw = request.getParameter("weekdayDietaId_" + day);
			if (raw == null || raw.isBlank()) {
				continue;
			}
			try {
				result.put(day, Long.parseLong(raw.trim()));
			}
			catch (final NumberFormatException ignored) {
				// skip invalid day entry
			}
		}
		return result;
	}

	public static PacienteDietaAssignmentType parseAssignmentType(final String raw) {
		if (raw == null || raw.isBlank()) {
			return PacienteDietaAssignmentType.DATE_RANGE;
		}
		try {
			return PacienteDietaAssignmentType.valueOf(raw.trim().toUpperCase());
		}
		catch (final IllegalArgumentException ex) {
			return PacienteDietaAssignmentType.DATE_RANGE;
		}
	}

}
