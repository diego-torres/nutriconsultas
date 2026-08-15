package com.nutriconsultas.dieta;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.nutriconsultas.alimentos.Alimento;

class AlimentoIngestaTest {

	@Test
	void getDisplayCantidad_scalesCatalogQuantityByPortions() {
		final Alimento alimento = new Alimento();
		alimento.setCantSugerida(1.0);
		final AlimentoIngesta alimentoIngesta = new AlimentoIngesta();
		alimentoIngesta.setAlimento(alimento);
		alimentoIngesta.setPortions(0.5);

		assertThat(alimentoIngesta.getDisplayCantidad()).isEqualTo("1/2");
	}

	@Test
	void getDisplayCantidad_formatsMixedFractions() {
		final Alimento alimento = new Alimento();
		alimento.setCantSugerida(1.0);
		final AlimentoIngesta alimentoIngesta = new AlimentoIngesta();
		alimentoIngesta.setAlimento(alimento);
		alimentoIngesta.setPortions(1.5);

		assertThat(alimentoIngesta.getDisplayCantidad()).isEqualTo("1 1/2");
	}

	@Test
	void getDisplayCantidad_showsCatalogFractionWhenPortionsAreOne() {
		final Alimento alimento = new Alimento();
		alimento.setCantSugerida(0.25);
		final AlimentoIngesta alimentoIngesta = new AlimentoIngesta();
		alimentoIngesta.setAlimento(alimento);
		alimentoIngesta.setPortions(1.0);

		assertThat(alimentoIngesta.getDisplayCantidad()).isEqualTo("1/4");
	}

}
