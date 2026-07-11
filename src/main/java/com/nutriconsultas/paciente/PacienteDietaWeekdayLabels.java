package com.nutriconsultas.paciente;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Spanish weekday labels for weekly diet assignment UI (#525).
 */
public final class PacienteDietaWeekdayLabels {

	public static final List<Integer> ISO_DAYS_MONDAY_FIRST = List.of(1, 2, 3, 4, 5, 6, 7);

	private static final Map<Integer, String> SPANISH_LABELS = Map.of(1, "Lunes", 2, "Martes", 3, "Miércoles", 4,
			"Jueves", 5, "Viernes", 6, "Sábado", 7, "Domingo");

	private PacienteDietaWeekdayLabels() {
	}

	public static String labelForDay(final int dayOfWeek) {
		return SPANISH_LABELS.getOrDefault(dayOfWeek, "Día " + dayOfWeek);
	}

	public static Map<Integer, PacienteDietaWeekday> slotsByDay(final List<PacienteDietaWeekday> slots) {
		final Map<Integer, PacienteDietaWeekday> byDay = new LinkedHashMap<>();
		if (slots == null) {
			return byDay;
		}
		for (final PacienteDietaWeekday slot : slots) {
			if (slot.getDayOfWeek() != null) {
				byDay.put(slot.getDayOfWeek(), slot);
			}
		}
		return byDay;
	}

}
