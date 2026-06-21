package com.nutriconsultas.booking;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Validates working-hours intervals and slot duration (#246).
 */
public final class WorkingHoursValidator {

	private WorkingHoursValidator() {
	}

	public static void validateSchedule(final AvailabilityScheduleDto schedule) {
		if (schedule == null) {
			throw new IllegalArgumentException("Horario no válido");
		}
		validateSlotDuration(schedule.getSlotDurationMinutes());
		validateTimezone(schedule.getTimezone());
		if (schedule.getIntervals() == null) {
			return;
		}
		for (final WorkingHoursIntervalDto interval : schedule.getIntervals()) {
			validateInterval(interval);
		}
		validateNoOverlaps(schedule.getIntervals());
	}

	public static void validateSlotDuration(final int slotDurationMinutes) {
		if (slotDurationMinutes < BookingAvailabilityConstants.MIN_SLOT_DURATION_MINUTES
				|| slotDurationMinutes > BookingAvailabilityConstants.MAX_SLOT_DURATION_MINUTES) {
			throw new IllegalArgumentException(
					"La duración de cita debe estar entre " + BookingAvailabilityConstants.MIN_SLOT_DURATION_MINUTES
							+ " y " + BookingAvailabilityConstants.MAX_SLOT_DURATION_MINUTES + " minutos");
		}
	}

	public static void validateTimezone(final String timezoneId) {
		if (timezoneId == null || timezoneId.isBlank()) {
			throw new IllegalArgumentException("La zona horaria es requerida");
		}
		try {
			java.time.ZoneId.of(timezoneId.trim());
		}
		catch (final java.time.DateTimeException ex) {
			throw new IllegalArgumentException("Zona horaria no válida: " + timezoneId, ex);
		}
	}

	private static void validateInterval(final WorkingHoursIntervalDto interval) {
		if (interval == null) {
			throw new IllegalArgumentException("Intervalo de horario no válido");
		}
		if (interval.getDayOfWeek() == null || interval.getDayOfWeek() < 1 || interval.getDayOfWeek() > 7) {
			throw new IllegalArgumentException("Día de la semana no válido");
		}
		final LocalTime start = interval.getStartTime();
		final LocalTime end = interval.getEndTime();
		if (start == null || end == null) {
			throw new IllegalArgumentException("Hora de inicio y fin son requeridas");
		}
		if (!start.isBefore(end)) {
			throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de fin");
		}
	}

	private static void validateNoOverlaps(final List<WorkingHoursIntervalDto> intervals) {
		final List<WorkingHoursIntervalDto> sorted = new ArrayList<>(intervals);
		sorted.sort(Comparator.comparing(WorkingHoursIntervalDto::getDayOfWeek)
			.thenComparing(WorkingHoursIntervalDto::getStartTime));
		for (int i = 1; i < sorted.size(); i++) {
			final WorkingHoursIntervalDto previous = sorted.get(i - 1);
			final WorkingHoursIntervalDto current = sorted.get(i);
			if (!previous.getDayOfWeek().equals(current.getDayOfWeek())) {
				continue;
			}
			if (current.getStartTime().isBefore(previous.getEndTime())) {
				throw new IllegalArgumentException("Los horarios se traslapan el mismo día");
			}
		}
	}

}
