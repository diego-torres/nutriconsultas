package com.nutriconsultas.mobile;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PatientAuthEmailMismatchException extends RuntimeException {

	public PatientAuthEmailMismatchException() {
		super("Signup email does not match invitation");
	}

}
