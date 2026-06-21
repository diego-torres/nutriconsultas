package com.nutriconsultas.booking;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Removes slot starts that overlap busy intervals (absence blocks, appointments) (#247).
 */
public final class BookingSlotFilter {

	private BookingSlotFilter() {
	}

	public static List<LocalTime> removeBusySlots(final List<LocalTime> slotStarts, final LocalDate date,
			final int slotDurationMinutes, final List<BusyTimeInterval> busyIntervals) {
		if (slotStarts == null || slotStarts.isEmpty()) {
			return List.of();
		}
		if (busyIntervals == null || busyIntervals.isEmpty()) {
			return List.copyOf(slotStarts);
		}
		final List<LocalTime> available = new ArrayList<>();
		for (final LocalTime slotStart : slotStarts) {
			final LocalDateTime start = date.atTime(slotStart);
			final LocalDateTime end = start.plusMinutes(slotDurationMinutes);
			if (!isBusy(start, end, busyIntervals)) {
				available.add(slotStart);
			}
		}
		return List.copyOf(available);
	}

	private static boolean isBusy(final LocalDateTime slotStart, final LocalDateTime slotEnd,
			final List<BusyTimeInterval> busyIntervals) {
		for (final BusyTimeInterval busy : busyIntervals) {
			if (busy.overlaps(slotStart, slotEnd)) {
				return true;
			}
		}
		return false;
	}

}
