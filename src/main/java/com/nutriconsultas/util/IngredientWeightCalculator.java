package com.nutriconsultas.util;

/**
 * Recalculates net weight (g) when portion quantity changes, using catalog reference
 * values.
 */
public final class IngredientWeightCalculator {

	private IngredientWeightCalculator() {
	}

	public static Integer recalculatePesoNeto(final Double referenceCantSugerida, final Integer referencePesoNeto,
			final Double newCantSugerida) {
		if (referenceCantSugerida == null || referenceCantSugerida == 0 || referencePesoNeto == null
				|| newCantSugerida == null) {
			return referencePesoNeto;
		}
		final double factor = newCantSugerida / referenceCantSugerida;
		return (int) Math.round(referencePesoNeto * factor);
	}

}
