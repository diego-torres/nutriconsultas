package com.nutriconsultas.paciente.calculation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TefCalculationServiceTest {

	@Test
	void calculateFixedTefOnGetDefaultPercent() {
		final Double tef = TefCalculationService.calculateTef(TefMethod.FIXED, TefBase.GET, null, null, null, null,
				1500.0, 2000.0);
		assertThat(tef).isEqualTo(200.0);
	}

	@Test
	void calculateFixedTefOnBmr() {
		final Double tef = TefCalculationService.calculateTef(TefMethod.FIXED, TefBase.BMR, 10.0, null, null, null,
				1500.0, 2000.0);
		assertThat(tef).isEqualTo(150.0);
	}

	@Test
	void calculateMacronutrientTefUsesConfiguredDistribution() {
		final Double tef = TefCalculationService.calculateTef(TefMethod.MACRONUTRIENTS, TefBase.GET, null, 30.0, 50.0,
				20.0, 1500.0, 2000.0);
		assertThat(tef).isEqualTo(2000.0 * (0.30 * 0.25 + 0.50 * 0.075 + 0.20 * 0.02));
	}

	@Test
	void calculateMacronutrientTefUsesBalancedDefaultWhenMacrosMissing() {
		final Double tef = TefCalculationService.calculateTef(TefMethod.MACRONUTRIENTS, TefBase.GET, null, null, null,
				null, 1500.0, 2000.0);
		assertThat(tef).isEqualTo(2000.0 * (0.30 * 0.25 + 0.50 * 0.075 + 0.20 * 0.02));
	}

	@Test
	void calculateTotalAdjustedKcalAddsTefToGet() {
		assertThat(TefCalculationService.calculateTotalAdjustedKcal(2000.0, 200.0)).isEqualTo(2200.0);
	}

	@Test
	void calculateTotalAdjustedKcalWithoutTefReturnsGet() {
		assertThat(TefCalculationService.calculateTotalAdjustedKcal(2000.0, null)).isEqualTo(2000.0);
	}

	@Test
	void calculateActivityKcal() {
		assertThat(TefCalculationService.calculateActivityKcal(1500.0, 2325.0)).isEqualTo(825.0);
	}

}
