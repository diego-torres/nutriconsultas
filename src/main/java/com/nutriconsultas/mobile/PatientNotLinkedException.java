package com.nutriconsultas.mobile;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class PatientNotLinkedException extends RuntimeException {

	public PatientNotLinkedException() {
		super("patient_not_linked");
	}

}
