package com.nutriconsultas.clinic;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateClinicInvitationForm {

	@NotBlank(message = "El correo es obligatorio")
	@Email(message = "Correo electrónico no válido")
	private String email;

}
