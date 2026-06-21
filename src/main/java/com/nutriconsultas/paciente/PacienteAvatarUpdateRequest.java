package com.nutriconsultas.paciente;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PacienteAvatarUpdateRequest {

	@NotBlank(message = "avatarId es requerido")
	private String avatarId;

}
