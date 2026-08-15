package com.nutriconsultas.dieta;

import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.util.FractionQuantityParser;

/**
 * Resolves the portion multiplier for a standalone alimento in a diet ingesta. The
 * multiplier is {@code cantidad / cantSugerida} so nutritionists can enter fractions of
 * the catalog suggested serving.
 */
public final class AlimentoIngestaPortions {

	public static final double MIN = 0.01;

	public static final double MAX = 100.0;

	public static final double DEFAULT = 1.0;

	private AlimentoIngestaPortions() {
	}

	public static double resolve(final AlimentoFormModel model, final Alimento alimento) {
		double result = DEFAULT;
		if (model != null) {
			final String tipoPorcion = AlimentoPortionDefaults.resolveTipoPorcion(model.getTipoPorcion());
			if (AlimentoPortionDefaults.PORCION.equals(tipoPorcion)) {
				final Double fromCantidad = ratioFromCantidad(model.getCantidad(), alimento);
				if (fromCantidad != null) {
					result = fromCantidad;
				}
				else if (model.getPorciones() != null) {
					result = model.getPorciones();
				}
			}
			else if (model.getPorciones() != null) {
				result = model.getPorciones();
			}
		}
		return clamp(result);
	}

	public static Double fromCantidad(final String cantidad, final Alimento alimento) {
		final Double ratio = ratioFromCantidad(cantidad, alimento);
		Double result = null;
		if (ratio != null) {
			result = clamp(ratio);
		}
		return result;
	}

	public static Double resolveUpdate(final String cantidad, final Double porciones, final Alimento alimento) {
		Double result = fromCantidad(cantidad, alimento);
		if (result == null && porciones != null) {
			result = clamp(porciones);
		}
		return result;
	}

	public static boolean isValid(final Double portions) {
		return portions != null && portions >= MIN && portions <= MAX;
	}

	public static double clamp(final double portions) {
		double result = portions;
		if (result < MIN) {
			result = MIN;
		}
		else if (result > MAX) {
			result = MAX;
		}
		return result;
	}

	private static Double ratioFromCantidad(final String cantidad, final Alimento alimento) {
		Double parsed;
		try {
			parsed = FractionQuantityParser.parseFractionalQuantity(cantidad);
		}
		catch (final NumberFormatException ex) {
			parsed = null;
		}
		Double result = null;
		if (parsed != null && parsed > 0) {
			result = parsed / catalogCantSugerida(alimento);
		}
		return result;
	}

	private static double catalogCantSugerida(final Alimento alimento) {
		double result = 1.0;
		if (alimento != null && alimento.getCantSugerida() != null && alimento.getCantSugerida() != 0) {
			result = alimento.getCantSugerida();
		}
		return result;
	}

}
