package com.nutriconsultas.mobile;

import java.io.Serial;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class PatientNotLinkedException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	public PatientNotLinkedException() {
		super("patient_not_linked");
	}

}
