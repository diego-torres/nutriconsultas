package com.nutriconsultas.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.nutriconsultas.alimentos.Alimento;

class AbstractFraccionableTest {

	@Test
	void getPesoUnitario_returnsNetWeightPerReferencePortion() {
		final Alimento alimento = new Alimento();
		alimento.setCantSugerida(2d);
		alimento.setPesoNeto(180);

		assertThat(alimento.getPesoUnitario()).isEqualTo(90d);
	}

	@Test
	void getPesoUnitario_returnsNullWhenReferenceQuantityMissing() {
		final Alimento alimento = new Alimento();
		alimento.setPesoNeto(180);

		assertThat(alimento.getPesoUnitario()).isNull();

		alimento.setCantSugerida(0d);
		assertThat(alimento.getPesoUnitario()).isNull();
	}

}
