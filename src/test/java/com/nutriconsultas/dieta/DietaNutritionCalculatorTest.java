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

	@Test
	void calculateNutrientTotalsRollsUpAllIngestaSnapshots() {
		final Dieta dieta = new Dieta();
		final Ingesta desayuno = new Ingesta();
		desayuno.setId(1L);
		desayuno.setNombre("Desayuno");
		final PlatilloIngesta platillo = new PlatilloIngesta();
		platillo.setProteina(20.0);
		platillo.setLipidos(10.0);
		platillo.setHidratosDeCarbono(50.0);
		platillo.setFibra(5.0);
		platillo.setSodio(300.0);
		desayuno.getPlatillos().add(platillo);

		final Ingesta cena = new Ingesta();
		cena.setId(2L);
		cena.setNombre("Cena");
		final AlimentoIngesta alimento = new AlimentoIngesta();
		alimento.setProteina(8.0);
		alimento.setCalcio(120.0);
		alimento.setVitA(50.0);
		cena.getAlimentos().add(alimento);

		dieta.getIngestas().add(desayuno);
		dieta.getIngestas().add(cena);

		final DietaNutrientTotals totals = DietaNutritionCalculator.calculateNutrientTotals(dieta);

		assertThat(totals.getProteina()).isEqualTo(28.0);
		assertThat(totals.getFibra()).isEqualTo(5.0);
		assertThat(totals.getSodio()).isEqualTo(300.0);
		assertThat(totals.getCalcio()).isEqualTo(120.0);
		assertThat(totals.getVitA()).isEqualTo(50.0);
	}

	@Test
	void calculateNutrientTotalsForIngestaSumsPlatillosAndAlimentos() {
		final Ingesta ingesta = new Ingesta();
		final PlatilloIngesta platillo = new PlatilloIngesta();
		platillo.setHierro(2.0);
		ingesta.getPlatillos().add(platillo);
		final AlimentoIngesta alimento = new AlimentoIngesta();
		alimento.setHierro(1.5);
		ingesta.getAlimentos().add(alimento);

		final DietaNutrientTotals totals = DietaNutritionCalculator.calculateNutrientTotals(ingesta);

		assertThat(totals.getHierro()).isEqualTo(3.5);
	}

}
