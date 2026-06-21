package com.nutriconsultas.booking;

public class PublicBookingNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PublicBookingNotFoundException() {
		super("Nutritionist booking link not found");
	}

}
