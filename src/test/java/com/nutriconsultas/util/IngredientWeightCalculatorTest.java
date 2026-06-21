package com.nutriconsultas.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IngredientWeightCalculatorTest {

	@Test
	void recalculatePesoNeto_scalesByQuantityRatio() {
		assertThat(IngredientWeightCalculator.recalculatePesoNeto(1d, 100, 2d)).isEqualTo(200);
	}

	@Test
	void recalculatePesoNeto_roundsToNearestGram() {
		assertThat(IngredientWeightCalculator.recalculatePesoNeto(2d, 100, 1.5d)).isEqualTo(75);
	}

	@Test
	void recalculatePesoNeto_returnsReferenceWhenInputsMissing() {
		assertThat(IngredientWeightCalculator.recalculatePesoNeto(null, 100, 2d)).isEqualTo(100);
		assertThat(IngredientWeightCalculator.recalculatePesoNeto(0d, 100, 2d)).isEqualTo(100);
		assertThat(IngredientWeightCalculator.recalculatePesoNeto(1d, null, 2d)).isNull();
		assertThat(IngredientWeightCalculator.recalculatePesoNeto(1d, 100, null)).isEqualTo(100);
	}

}
