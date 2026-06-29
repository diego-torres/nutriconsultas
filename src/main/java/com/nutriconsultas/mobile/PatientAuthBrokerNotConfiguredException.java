package com.nutriconsultas.mobile;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class PatientAuthBrokerNotConfiguredException extends RuntimeException {

	public PatientAuthBrokerNotConfiguredException() {
		super("Patient auth broker is not configured");
	}

}
