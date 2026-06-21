package com.nutriconsultas.booking;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Generates bookable slot start times within configured working hours (#246).
 */
public final class BookingSlotGenerator {

	private BookingSlotGenerator() {
	}

	/**
	 * @param intervals working windows for the target weekday (may span multiple rows)
	 * @param slotDurationMinutes appointment length in minutes
	 * @return sorted slot start times that fit entirely inside an interval
	 */
	public static List<LocalTime> generateSlotStarts(final List<WorkingHoursIntervalDto> intervals,
			final int slotDurationMinutes) {
		WorkingHoursValidator.validateSlotDuration(slotDurationMinutes);
		if (intervals == null || intervals.isEmpty()) {
			return List.of();
		}
		final List<LocalTime> slotStarts = new ArrayList<>();
		final List<WorkingHoursIntervalDto> sorted = new ArrayList<>(intervals);
		sorted.sort(Comparator.comparing(WorkingHoursIntervalDto::getStartTime));
		for (final WorkingHoursIntervalDto interval : sorted) {
			if (interval.getStartTime() == null || interval.getEndTime() == null) {
				continue;
			}
			LocalTime candidate = interval.getStartTime();
			while (!candidate.plusMinutes(slotDurationMinutes).isAfter(interval.getEndTime())) {
				slotStarts.add(candidate);
				candidate = candidate.plusMinutes(slotDurationMinutes);
			}
		}
		slotStarts.sort(LocalTime::compareTo);
		return List.copyOf(slotStarts);
	}

}
