package com.nutriconsultas.booking;

/**
 * Sends (or prepares) patient confirmation after public self-booking (#302).
 */
public interface PublicBookingConfirmationEmailSender {

	void sendConfirmation(String recipientEmail, PublicBookingConfirmationEmailDetails details);

}
