package com.nutriconsultas.paciente;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PacienteAvatarCatalogTest {

	@Test
	void resolveImagePath_usesStoredAvatarWhenValid() {
		final Paciente paciente = new Paciente();
		paciente.setGender("M");
		paciente.setAvatarId("avatar_8");

		assertThat(PacienteAvatarCatalog.resolveImagePath(paciente))
			.isEqualTo("/sbadmin/img/paciente-avatars/avatar_8.png");
		assertThat(PacienteAvatarCatalog.resolveSelectedId(paciente)).isEqualTo("avatar_8");
	}

	@Test
	void resolveImagePath_fallsBackToGenderDefaultWhenUnset() {
		final Paciente male = new Paciente();
		male.setGender("M");
		final Paciente female = new Paciente();
		female.setGender("F");

		assertThat(PacienteAvatarCatalog.resolveImagePath(male))
			.isEqualTo("/sbadmin/img/paciente-avatars/avatar_1.png");
		assertThat(PacienteAvatarCatalog.resolveImagePath(female))
			.isEqualTo("/sbadmin/img/paciente-avatars/avatar_6.png");
	}

	@Test
	void isValid_acceptsIllustratedSetAndRejectsUnknownKeys() {
		assertThat(PacienteAvatarCatalog.isValid("avatar_1")).isTrue();
		assertThat(PacienteAvatarCatalog.isValid("avatar_10")).isTrue();
		assertThat(PacienteAvatarCatalog.isValid("kid_avatar_10")).isTrue();
		assertThat(PacienteAvatarCatalog.isValid("avatar01")).isFalse();
		assertThat(PacienteAvatarCatalog.isValid("ava3")).isFalse();
		assertThat(PacienteAvatarCatalog.isValid("unknown")).isFalse();
		assertThat(PacienteAvatarCatalog.allOptions()).hasSize(20);
		assertThat(PacienteAvatarCatalog.adultOptions()).hasSize(10);
		assertThat(PacienteAvatarCatalog.kidOptions()).hasSize(10);
	}

	@Test
	void resolveSelectedId_normalizesLegacyStoredKeys() {
		final Paciente paciente = new Paciente();
		paciente.setGender("M");
		paciente.setAvatarId("avatar08");

		assertThat(PacienteAvatarCatalog.resolveSelectedId(paciente)).isEqualTo("avatar_8");
		assertThat(PacienteAvatarCatalog.resolveImagePath(paciente))
			.isEqualTo("/sbadmin/img/paciente-avatars/avatar_8.png");
	}

	@Test
	void resolveSelectedId_normalizesKidLegacyKeys() {
		final Paciente paciente = new Paciente();
		paciente.setGender("F");
		paciente.setAvatarId("avatar15");

		assertThat(PacienteAvatarCatalog.resolveSelectedId(paciente)).isEqualTo("kid_avatar_5");
		assertThat(PacienteAvatarCatalog.resolveImagePath(paciente))
			.isEqualTo("/sbadmin/img/paciente-avatars/kid_avatar_5.png");
	}

}
