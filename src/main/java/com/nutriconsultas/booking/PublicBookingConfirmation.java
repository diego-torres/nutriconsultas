package com.nutriconsultas.booking;

/**
 * Result of a successful public booking (#248).
 */
public record PublicBookingConfirmation(long eventId, String date, String time) {
}
