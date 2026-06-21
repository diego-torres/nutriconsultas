package com.nutriconsultas.booking;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PublicBookingRequestDto {

	@NotBlank(message = "El nombre es obligatorio")
	@Size(max = 100)
	private String patientName;

	@NotBlank(message = "El correo es obligatorio")
	@Email(message = "Correo no válido")
	@Size(max = 100)
	private String patientEmail;

	@Size(max = 30)
	private String patientPhone;

	@NotNull(message = "La fecha es obligatoria")
	private String date;

	@NotBlank(message = "La hora es obligatoria")
	private String time;

	@NotBlank(message = "Complete la verificación reCAPTCHA")
	private String recaptchaResponse;

}
