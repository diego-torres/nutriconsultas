package com.nutriconsultas.dieta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class IngestaNamesTest {

	@Test
	void hasDuplicateNameIsCaseInsensitive() {
		final Ingesta existing = new Ingesta();
		existing.setId(1L);
		existing.setNombre("Colacion");

		assertThat(IngestaNames.hasDuplicateName(List.of(existing), "colacion", null)).isTrue();
		assertThat(IngestaNames.hasDuplicateName(List.of(existing), "Colacion 2", null)).isFalse();
	}

	@Test
	void validateForDietaRejectsBlankName() {
		assertThatThrownBy(() -> IngestaNames.validateForDieta(List.of(), "  ", null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("requerido");
	}

	@Test
	void validateForDietaAllowsRenameToSameName() {
		final Ingesta existing = new Ingesta();
		existing.setId(1L);
		existing.setNombre("Desayuno");

		IngestaNames.validateForDieta(List.of(existing), "desayuno", 1L);
	}

}
