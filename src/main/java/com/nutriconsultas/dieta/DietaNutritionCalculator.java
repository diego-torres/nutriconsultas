package com.nutriconsultas.dieta;

/**
 * Calculates dieta totals from ingestas (platillos and alimentos). Same formula as
 * {@code /admin/dietas} grid: protein × 4 + lipids × 9 + carbohydrates × 4.
 */
public final class DietaNutritionCalculator {

	private DietaNutritionCalculator() {
	}

	public static Double calculateTotalProteina(final Dieta dieta) {
		if (dieta == null || dieta.getIngestas() == null) {
			return 0.0;
		}
		return dieta.getIngestas().stream().mapToDouble(ingesta -> {
			final double platillos = ingesta.getPlatillos() != null ? ingesta.getPlatillos()
				.stream()
				.mapToDouble(p -> p.getProteina() != null ? p.getProteina() : 0.0)
				.sum() : 0.0;
			final double alimentos = ingesta.getAlimentos() != null ? ingesta.getAlimentos()
				.stream()
				.mapToDouble(a -> a.getProteina() != null ? a.getProteina() : 0.0)
				.sum() : 0.0;
			return platillos + alimentos;
		}).sum();
	}

	public static Double calculateTotalLipidos(final Dieta dieta) {
		if (dieta == null || dieta.getIngestas() == null) {
			return 0.0;
		}
		return dieta.getIngestas().stream().mapToDouble(ingesta -> {
			final double platillos = ingesta.getPlatillos() != null ? ingesta.getPlatillos()
				.stream()
				.mapToDouble(p -> p.getLipidos() != null ? p.getLipidos() : 0.0)
				.sum() : 0.0;
			final double alimentos = ingesta.getAlimentos() != null ? ingesta.getAlimentos()
				.stream()
				.mapToDouble(a -> a.getLipidos() != null ? a.getLipidos() : 0.0)
				.sum() : 0.0;
			return platillos + alimentos;
		}).sum();
	}

	public static Double calculateTotalHidratosDeCarbono(final Dieta dieta) {
		if (dieta == null || dieta.getIngestas() == null) {
			return 0.0;
		}
		return dieta.getIngestas().stream().mapToDouble(ingesta -> {
			final double platillos = ingesta.getPlatillos() != null ? ingesta.getPlatillos()
				.stream()
				.mapToDouble(p -> p.getHidratosDeCarbono() != null ? p.getHidratosDeCarbono() : 0.0)
				.sum() : 0.0;
			final double alimentos = ingesta.getAlimentos() != null ? ingesta.getAlimentos()
				.stream()
				.mapToDouble(a -> a.getHidratosDeCarbono() != null ? a.getHidratosDeCarbono() : 0.0)
				.sum() : 0.0;
			return platillos + alimentos;
		}).sum();
	}

	public static Double calculateTotalKcal(final Dieta dieta) {
		if (dieta == null) {
			return 0.0;
		}
		final Double proteina = calculateTotalProteina(dieta);
		final Double lipidos = calculateTotalLipidos(dieta);
		final Double hidratos = calculateTotalHidratosDeCarbono(dieta);
		return proteina * 4 + lipidos * 9 + hidratos * 4;
	}

	public static void applyCalculatedNutrients(final Dieta dieta) {
		if (dieta == null) {
			return;
		}
		dieta.setProteina(calculateTotalProteina(dieta));
		dieta.setLipidos(calculateTotalLipidos(dieta));
		dieta.setHidratosDeCarbono(calculateTotalHidratosDeCarbono(dieta));
		final Double kcal = calculateTotalKcal(dieta);
		dieta.setEnergia(kcal != null ? kcal.intValue() : 0);
	}

	public static DietaNutrientTotals calculateNutrientTotals(final Dieta dieta) {
		final DietaNutrientTotals totals = new DietaNutrientTotals();
		if (dieta == null || dieta.getIngestas() == null) {
			return totals;
		}
		for (final Ingesta ingesta : dieta.getIngestas()) {
			totals.addTotals(calculateNutrientTotals(ingesta));
		}
		return totals;
	}

	public static DietaNutrientTotals calculateNutrientTotals(final Ingesta ingesta) {
		final DietaNutrientTotals totals = new DietaNutrientTotals();
		if (ingesta == null) {
			return totals;
		}
		if (ingesta.getPlatillos() != null) {
			for (final PlatilloIngesta platillo : ingesta.getPlatillos()) {
				totals.addFrom(platillo);
			}
		}
		if (ingesta.getAlimentos() != null) {
			for (final AlimentoIngesta alimento : ingesta.getAlimentos()) {
				totals.addFromAlimento(alimento);
			}
		}
		return totals;
	}

}
