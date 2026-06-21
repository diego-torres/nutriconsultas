package com.nutriconsultas.booking;

import java.time.LocalDate;
import java.util.List;

/**
 * Public slots response for a single day (#248).
 */
public record PublicBookingSlotsResponse(String date, LocalDate minBookableDate, int minAdvanceDays, List<String> slots,
		String notice) {
}
