package com.nutriconsultas.booking;

import java.time.LocalDateTime;

/**
 * Half-open interval {@code [start, end)} used when subtracting busy windows from
 * bookable slots.
 */
public record BusyTimeInterval(LocalDateTime start, LocalDateTime end) {

	public BusyTimeInterval {
		if (start == null || end == null) {
			throw new IllegalArgumentException("Busy interval bounds are required");
		}
		if (!end.isAfter(start)) {
			throw new IllegalArgumentException("Busy interval end must be after start");
		}
	}

	public boolean overlaps(final LocalDateTime intervalStart, final LocalDateTime intervalEnd) {
		return intervalStart.isBefore(end) && intervalEnd.isAfter(start);
	}

}
