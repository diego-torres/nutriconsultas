package com.nutriconsultas.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Exposes the Maven-filtered application version to admin Thymeleaf layouts (Acerca de).
 */
@Component
@ControllerAdvice
public class AppVersionModelAdvice {

	private final String configuredVersion;

	public AppVersionModelAdvice(@Value("${app.version:unknown}") final String configuredVersion) {
		this.configuredVersion = configuredVersion;
	}

	@ModelAttribute("appVersion")
	public String appVersion() {
		return this.configuredVersion;
	}

}
