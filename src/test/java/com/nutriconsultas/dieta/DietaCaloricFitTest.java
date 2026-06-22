package com.nutriconsultas.dieta;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DietaCaloricFitTest {

	@Test
	void classify_returnsMatchWithinTolerance() {
		assertThat(DietaCaloricFit.classify(2000, 2000.0)).isEqualTo(DietaCaloricFit.Fit.MATCH);
		assertThat(DietaCaloricFit.classify(1980, 2000.0)).isEqualTo(DietaCaloricFit.Fit.MATCH);
	}

	@Test
	void classify_returnsUnderWhenBelowRequirement() {
		assertThat(DietaCaloricFit.classify(1700, 2000.0)).isEqualTo(DietaCaloricFit.Fit.UNDER);
	}

	@Test
	void classify_returnsOverWhenAboveRequirement() {
		assertThat(DietaCaloricFit.classify(2300, 2000.0)).isEqualTo(DietaCaloricFit.Fit.OVER);
	}

	@Test
	void classify_returnsUnknownWithoutRequirement() {
		assertThat(DietaCaloricFit.classify(1800, null)).isEqualTo(DietaCaloricFit.Fit.UNKNOWN);
	}

	@Test
	void label_describesFitInSpanish() {
		assertThat(DietaCaloricFit.label(DietaCaloricFit.Fit.MATCH, 2000, 2000.0)).isEqualTo("Adecuada");
		assertThat(DietaCaloricFit.label(DietaCaloricFit.Fit.UNDER, 1700, 2000.0)).isEqualTo("Por debajo (300 kcal)");
		assertThat(DietaCaloricFit.label(DietaCaloricFit.Fit.OVER, 2300, 2000.0)).isEqualTo("Por encima (+300 kcal)");
	}

}
