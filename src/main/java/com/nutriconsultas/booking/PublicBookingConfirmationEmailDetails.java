package com.nutriconsultas.booking;

/**
 * Non-PHI payload for public booking confirmation emails (#302).
 */
public record PublicBookingConfirmationEmailDetails(String patientName, String nutritionistDisplayName,
		String appointmentDateFormatted, String appointmentTimeFormatted) {

}
