package com.nutriconsultas.booking;

import org.springframework.lang.NonNull;

public class PublicBookingNotFoundException extends RuntimeException {

	public PublicBookingNotFoundException() {
		super("Nutritionist booking link not found");
	}

}
