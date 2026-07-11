package com.nutriconsultas.dieta;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AlimentoPortionDefaultsTest {

	@Test
	void resolveTipoPorcion_defaultsToPorcionWhenNullOrBlank() {
		assertThat(AlimentoPortionDefaults.resolveTipoPorcion(null)).isEqualTo(AlimentoPortionDefaults.PORCION);
		assertThat(AlimentoPortionDefaults.resolveTipoPorcion("")).isEqualTo(AlimentoPortionDefaults.PORCION);
		assertThat(AlimentoPortionDefaults.resolveTipoPorcion("   ")).isEqualTo(AlimentoPortionDefaults.PORCION);
	}

	@Test
	void resolveTipoPorcion_preservesExplicitValue() {
		assertThat(AlimentoPortionDefaults.resolveTipoPorcion(AlimentoPortionDefaults.GRAMOS))
			.isEqualTo(AlimentoPortionDefaults.GRAMOS);
		assertThat(AlimentoPortionDefaults.resolveTipoPorcion(AlimentoPortionDefaults.PORCION))
			.isEqualTo(AlimentoPortionDefaults.PORCION);
	}

}
