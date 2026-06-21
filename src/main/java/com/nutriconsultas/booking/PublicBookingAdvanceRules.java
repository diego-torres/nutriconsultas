package com.nutriconsultas.booking;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Public booking lead-time rules (#248). Nutritionist admin preview endpoints do not use
 * these checks.
 */
public final class PublicBookingAdvanceRules {

	private PublicBookingAdvanceRules() {
	}

	public static LocalDate earliestBookableDate(final ZoneId zoneId) {
		return LocalDate.now(zoneId).plusDays(BookingAvailabilityConstants.MIN_BOOKING_ADVANCE_DAYS);
	}

	public static boolean isDateBookable(final LocalDate date, final ZoneId zoneId) {
		return date != null && !date.isBefore(earliestBookableDate(zoneId));
	}

	public static boolean isSlotBookable(final LocalDateTime slotStart, final ZoneId zoneId) {
		return slotStart != null && !slotStart.toLocalDate().isBefore(earliestBookableDate(zoneId));
	}

}
