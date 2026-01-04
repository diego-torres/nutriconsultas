package com.nutriconsultas.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for contact form submissions.
 */
@Data
public class ContactForm {

	@NotBlank(message = "El nombre es requerido")
	@Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
	private String name;

	@NotBlank(message = "El email es requerido")
	@Email(message = "El email debe ser válido")
	@Size(max = 255, message = "El email no puede exceder 255 caracteres")
	private String email;

	@NotBlank(message = "El asunto es requerido")
	@Size(max = 200, message = "El asunto no puede exceder 200 caracteres")
	private String subject;

	@NotBlank(message = "El mensaje es requerido")
	@Size(max = 2000, message = "El mensaje no puede exceder 2000 caracteres")
	private String message;

	private String recaptchaResponse;

}
