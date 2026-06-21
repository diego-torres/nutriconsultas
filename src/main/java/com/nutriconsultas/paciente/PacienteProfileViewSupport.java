package com.nutriconsultas.paciente;

import org.springframework.ui.Model;

/**
 * Shared Thymeleaf model attributes for patient profile views (#241).
 */
public final class PacienteProfileViewSupport {

	private PacienteProfileViewSupport() {
	}

	public static void addAvatarAttributes(final Model model, final Paciente paciente) {
		model.addAttribute("avatarImageUrl", PacienteAvatarCatalog.resolveImagePath(paciente));
		model.addAttribute("selectedAvatarId", PacienteAvatarCatalog.resolveSelectedId(paciente));
		model.addAttribute("avatarOptions", PacienteAvatarCatalog.allOptions());
	}

}
