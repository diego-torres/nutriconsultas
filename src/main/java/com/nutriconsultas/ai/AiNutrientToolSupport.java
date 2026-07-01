package com.nutriconsultas.ai;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.model.AbstractFraccionable;
import com.nutriconsultas.platillos.Ingrediente;
import com.nutriconsultas.util.IngredienteFromAlimentoCalculator;

/**
 * Shared nutrient calculation helpers for AI catalog tools.
 */
public final class AiNutrientToolSupport {

	private AiNutrientToolSupport() {
	}

	@Nullable
	public static Ingrediente calculateIngredient(final Alimento alimento, final String trimmedCantidad,
			@Nullable final Integer pesoNetoG) {
		final Ingrediente ingrediente = new Ingrediente();
		try {
			final Integer effectivePeso = pesoNetoG != null ? pesoNetoG : alimento.getPesoNeto();
			IngredienteFromAlimentoCalculator.populateCatalogIngredienteFromAlimento(ingrediente, alimento,
					trimmedCantidad, effectivePeso);
		}
		catch (IllegalArgumentException | ArithmeticException ex) {
			return null;
		}
		return ingrediente;
	}

	public static boolean isUnitSupported(final Alimento alimento, @Nullable final String requestedUnit) {
		return !StringUtils.hasText(requestedUnit) || (StringUtils.hasText(alimento.getUnidad())
				&& alimento.getUnidad().trim().equalsIgnoreCase(requestedUnit.trim()));
	}

	public static NutrientSummary toNutrientSummary(final AbstractFraccionable source) {
		return new NutrientSummary(source.getEnergia(), source.getProteina(), source.getLipidos(),
				source.getHidratosDeCarbono(), source.getFibra(), source.getSodio(), source.getPotasio());
	}

	public static NutrientSummary scaleNutrientSummary(final NutrientSummary summary, final int portions) {
		if (portions == 1) {
			return summary;
		}
		return new NutrientSummary(scaleInteger(summary.energiaKcal(), portions),
				scaleDouble(summary.proteinaG(), portions), scaleDouble(summary.lipidosG(), portions),
				scaleDouble(summary.hidratosDeCarbonoG(), portions), scaleDouble(summary.fibraG(), portions),
				scaleDouble(summary.sodioMg(), portions), scaleDouble(summary.potasioMg(), portions));
	}

	public static NutrientSummary divideNutrientSummary(final NutrientSummary summary, final int portions) {
		if (portions == 1) {
			return summary;
		}
		return new NutrientSummary(divideInteger(summary.energiaKcal(), portions),
				divideDouble(summary.proteinaG(), portions), divideDouble(summary.lipidosG(), portions),
				divideDouble(summary.hidratosDeCarbonoG(), portions), divideDouble(summary.fibraG(), portions),
				divideDouble(summary.sodioMg(), portions), divideDouble(summary.potasioMg(), portions));
	}

	@Nullable
	private static Integer scaleInteger(@Nullable final Integer value, final int portions) {
		if (value == null) {
			return null;
		}
		return value * portions;
	}

	@Nullable
	private static Double scaleDouble(@Nullable final Double value, final int portions) {
		if (value == null) {
			return null;
		}
		return value * portions;
	}

	@Nullable
	private static Integer divideInteger(@Nullable final Integer value, final int portions) {
		if (value == null) {
			return null;
		}
		return (int) Math.round((double) value / portions);
	}

	@Nullable
	private static Double divideDouble(@Nullable final Double value, final int portions) {
		if (value == null) {
			return null;
		}
		return value / portions;
	}

}
