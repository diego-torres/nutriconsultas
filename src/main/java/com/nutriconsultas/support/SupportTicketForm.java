package com.nutriconsultas.support;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Create-ticket form for the nutritionist Soporte page.
 */
public class SupportTicketForm {

	@NotBlank(message = "El título es obligatorio")
	@Size(max = 200, message = "El título no puede exceder 200 caracteres")
	private String title;

	@NotBlank(message = "La descripción es obligatoria")
	@Size(max = 4000, message = "La descripción no puede exceder 4000 caracteres")
	private String description;

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

}
