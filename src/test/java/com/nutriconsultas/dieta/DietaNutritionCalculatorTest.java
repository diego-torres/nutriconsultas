package com.nutriconsultas.dieta;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DietaNutritionCalculatorTest {

	@Test
	void calculateTotalKcalUsesMacroFormula() {
		final Dieta dieta = new Dieta();
		final Ingesta ingesta = new Ingesta();
		final PlatilloIngesta platillo = new PlatilloIngesta();
		platillo.setProteina(20.0);
		platillo.setLipidos(10.0);
		platillo.setHidratosDeCarbono(50.0);
		ingesta.getPlatillos().add(platillo);
		dieta.getIngestas().add(ingesta);

		assertThat(DietaNutritionCalculator.calculateTotalKcal(dieta)).isEqualTo(370.0);
	}

}
