package com.nutriconsultas.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.nutriconsultas.alimentos.Alimento;

/**
 * Rounded fractions pick the nearest value among wholes, quarters, thirds, and halves.
 * Midpoints between adjacent standard fractions (e.g. 0.2917 between 1/4 and 1/3) may
 * resolve to either neighbor depending on floating-point distance; tie-break prefers the
 * simpler denominator.
 */
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

	@ParameterizedTest
	@CsvSource({ "0.33, 1/3", "0.5, 1/2", "0.25, 1/4", "1.67, '1 2/3'", "1.0, 1", "2.75, '2 3/4'", "0.667, 2/3",
			"0.75, 3/4", "1.25, '1 1/4'", "1.5, '1 1/2'", "3.0, 3" })
	void getRoundedFractionalCantSugerida_roundsToStandardCookingFractions(final double cantSugerida,
			final String expected) {
		final Alimento alimento = new Alimento();
		alimento.setCantSugerida(cantSugerida);

		assertThat(alimento.getRoundedFractionalCantSugerida()).isEqualTo(expected);
	}

	@Test
	void getRoundedFractionalCantSugerida_roundsAwkwardRatiosToNearestStandardFraction() {
		final Alimento alimento = new Alimento();
		alimento.setCantSugerida(6d / 23d);

		assertThat(alimento.getRoundedFractionalCantSugerida()).isEqualTo("1/4");
	}

	@Test
	void getFractionalCantSugerida_delegatesToRoundedFormatter() {
		final Alimento alimento = new Alimento();
		alimento.setCantSugerida(0.33);

		assertThat(alimento.getFractionalCantSugerida()).isEqualTo(alimento.getRoundedFractionalCantSugerida());
		assertThat(alimento.getFractionalCantSugerida()).isEqualTo("1/3");
	}

	@Test
	void getRoundedFractionalCantSugerida_returnsEmptyStringWhenQuantityMissing() {
		final Alimento alimento = new Alimento();

		assertThat(alimento.getRoundedFractionalCantSugerida()).isEmpty();
		assertThat(alimento.getFractionalCantSugerida()).isEmpty();
	}

	@Test
	void getDisplayCantSugerida_usesRoundedGramsForGramUnit() {
		final Alimento alimento = new Alimento();
		alimento.setCantSugerida(59.469025);
		alimento.setPesoBrutoRedondeado(59);

		assertThat(alimento.shouldDisplayWeightInGrams("g")).isTrue();
		assertThat(alimento.getDisplayCantSugerida("g")).isEqualTo("59");
		assertThat(alimento.getRoundedFractionalCantSugerida()).isEqualTo("59 1/2");
	}

	@Test
	void shouldDisplayWeightInGrams_usesGramFallbackForSmallTazaPortions() {
		final Alimento alimento = new Alimento();
		alimento.setCantSugerida(0.2);
		alimento.setPesoBrutoRedondeado(45);

		assertThat(alimento.shouldDisplayWeightInGrams("taza")).isTrue();
		assertThat(alimento.getDisplayCantSugerida("taza")).isEqualTo("45");
	}

	@Test
	void shouldDisplayWeightInGrams_doesNotApplyToRegularTazaPortions() {
		final Alimento alimento = new Alimento();
		alimento.setCantSugerida(0.33);
		alimento.setPesoBrutoRedondeado(75);

		assertThat(alimento.shouldDisplayWeightInGrams("taza")).isFalse();
		assertThat(alimento.getDisplayCantSugerida("taza")).isEqualTo("1/3");
	}

}
