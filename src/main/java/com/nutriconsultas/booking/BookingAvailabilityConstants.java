package com.nutriconsultas.booking;

import java.time.ZoneId;

/**
 * Defaults and bounds for nutritionist public-booking availability (#246).
 */
public final class BookingAvailabilityConstants {

	public static final ZoneId DEFAULT_TIMEZONE = ZoneId.of("America/Mexico_City");

	public static final String DEFAULT_TIMEZONE_ID = DEFAULT_TIMEZONE.getId();

	public static final int DEFAULT_SLOT_DURATION_MINUTES = 60;

	public static final int MIN_SLOT_DURATION_MINUTES = 15;

	public static final int MAX_SLOT_DURATION_MINUTES = 120;

	public static final int MIN_BOOKING_ADVANCE_DAYS = 2;

	private BookingAvailabilityConstants() {
	}

}
