package com.nutriconsultas.dieta;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.nutriconsultas.alimentos.Alimento;

class AlimentoIngestaPortionsTest {

	@Test
	void resolve_usesCantidadRatioAgainstCatalogSuggestedServing() {
		final Alimento alimento = catalogAlimento(1.0);
		final AlimentoFormModel model = new AlimentoFormModel();
		model.setTipoPorcion(AlimentoPortionDefaults.PORCION);
		model.setCantidad("1/2");
		model.setPorciones(1.0);

		assertThat(AlimentoIngestaPortions.resolve(model, alimento)).isEqualTo(0.5);
	}

	@Test
	void resolve_keepsOnePortionWhenCantidadMatchesSuggestedServing() {
		final Alimento alimento = catalogAlimento(0.5);
		final AlimentoFormModel model = new AlimentoFormModel();
		model.setTipoPorcion(AlimentoPortionDefaults.PORCION);
		model.setCantidad("1/2");

		assertThat(AlimentoIngestaPortions.resolve(model, alimento)).isEqualTo(1.0);
	}

	@Test
	void resolve_fallsBackToPorcionesWhenCantidadMissing() {
		final AlimentoFormModel model = new AlimentoFormModel();
		model.setTipoPorcion(AlimentoPortionDefaults.PORCION);
		model.setPorciones(2.0);

		assertThat(AlimentoIngestaPortions.resolve(model, catalogAlimento(1.0))).isEqualTo(2.0);
	}

	@Test
	void resolve_usesPorcionesForGramosType() {
		final AlimentoFormModel model = new AlimentoFormModel();
		model.setTipoPorcion(AlimentoPortionDefaults.GRAMOS);
		model.setCantidad("1/2");
		model.setPorciones(2.0);

		assertThat(AlimentoIngestaPortions.resolve(model, catalogAlimento(1.0))).isEqualTo(2.0);
	}

	@Test
	void resolve_clampsValuesOutsideAllowedRange() {
		final AlimentoFormModel tooSmall = new AlimentoFormModel();
		tooSmall.setTipoPorcion(AlimentoPortionDefaults.PORCION);
		tooSmall.setCantidad("0.001");

		final AlimentoFormModel tooLarge = new AlimentoFormModel();
		tooLarge.setTipoPorcion(AlimentoPortionDefaults.GRAMOS);
		tooLarge.setPorciones(200.0);

		assertThat(AlimentoIngestaPortions.resolve(tooSmall, catalogAlimento(1.0)))
			.isEqualTo(AlimentoIngestaPortions.MIN);
		assertThat(AlimentoIngestaPortions.resolve(tooLarge, catalogAlimento(1.0)))
			.isEqualTo(AlimentoIngestaPortions.MAX);
	}

	@Test
	void isValid_acceptsFractionalPortionsWithinRange() {
		assertThat(AlimentoIngestaPortions.isValid(0.5)).isTrue();
		assertThat(AlimentoIngestaPortions.isValid(0.1)).isTrue();
		assertThat(AlimentoIngestaPortions.isValid(0.001)).isFalse();
		assertThat(AlimentoIngestaPortions.isValid(null)).isFalse();
	}

	@Test
	void fromCantidad_keepsOnePortionWhenEnteredQuantityMatchesCatalogServing() {
		final Alimento amaranto = catalogAlimento(0.25);

		assertThat(AlimentoIngestaPortions.fromCantidad("1/4", amaranto)).isEqualTo(1.0);
		assertThat(AlimentoIngestaPortions.fromCantidad("1", amaranto)).isEqualTo(4.0);
	}

	@Test
	void fromCantidad_acceptsSmallFractionOfUnitCatalogServing() {
		assertThat(AlimentoIngestaPortions.fromCantidad("1/8", catalogAlimento(1.0))).isEqualTo(0.125);
	}

	@Test
	void resolveUpdate_prefersCantidadOverPorciones() {
		final Double portions = AlimentoIngestaPortions.resolveUpdate("1/2", 3.0, catalogAlimento(1.0));

		assertThat(portions).isEqualTo(0.5);
	}

	@Test
	void resolveUpdate_fallsBackToPorcionesWhenCantidadMissing() {
		assertThat(AlimentoIngestaPortions.resolveUpdate(null, 2.0, catalogAlimento(1.0))).isEqualTo(2.0);
	}

	private static Alimento catalogAlimento(final double cantSugerida) {
		final Alimento alimento = new Alimento();
		alimento.setCantSugerida(cantSugerida);
		return alimento;
	}

}
