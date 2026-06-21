package com.nutriconsultas.booking;

/**
 * Resolved nutritionist context for a public booking page (no internal {@code userId} in
 * responses).
 */
public record PublicBookingNutritionistContext(String publicBookingId, String displayName, String timezone,
		int minAdvanceDays) {
}
