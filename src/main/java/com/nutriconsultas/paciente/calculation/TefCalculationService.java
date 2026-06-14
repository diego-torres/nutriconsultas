package com.nutriconsultas.paciente.calculation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

/**
 * Thermic effect of food (TEF / ETA) calculations.
 *
 * <p>
 * <strong>Fixed method:</strong> TEF = base × (percent / 100), where base is GET or TMB
 * per {@link TefBase}.
 *
 * <p>
 * <strong>Macronutrient method:</strong> TEF = GET × weighted TEF coefficients (protein
 * ~25%, carbohydrates ~7.5%, fat ~2% of each macro's caloric contribution).
 */
@Service
@Slf4j
public final class TefCalculationService {

	/** Default fixed TEF when no percent is configured (10% of base). */
	public static final double DEFAULT_FIXED_TEF_PERCENT = 10.0;

	private static final double PROTEIN_TEF_COEFFICIENT = 0.25;

	private static final double CARBOHYDRATE_TEF_COEFFICIENT = 0.075;

	private static final double FAT_TEF_COEFFICIENT = 0.02;

	private TefCalculationService() {
		// Utility class
	}

	/**
	 * Calculates TEF in kcal/day from patient preferences and computed GET/BMR.
	 */
	public static Double calculateTef(final TefMethod method, final TefBase base, final Double fixedPercent,
			final Double proteinPercent, final Double carbsPercent, final Double fatPercent, final Double bmr,
			final Double getKcal) {
		if (bmr == null && getKcal == null) {
			return null;
		}
		final TefMethod resolvedMethod = method != null ? method : TefMethod.FIXED;
		if (resolvedMethod == TefMethod.MACRONUTRIENTS) {
			return calculateMacronutrientTef(getKcal, proteinPercent, carbsPercent, fatPercent);
		}
		return calculateFixedTef(base, fixedPercent, bmr, getKcal);
	}

	/**
	 * Total daily energy requirement including TEF: GET + TEF.
	 */
	public static Double calculateTotalAdjustedKcal(final Double getKcal, final Double tefKcal) {
		if (getKcal == null) {
			return null;
		}
		if (tefKcal == null) {
			return getKcal;
		}
		return getKcal + tefKcal;
	}

	/**
	 * Activity energy beyond basal metabolism (GET − TMB).
	 */
	public static Double calculateActivityKcal(final Double bmr, final Double getKcal) {
		if (bmr == null || getKcal == null) {
			return null;
		}
		return getKcal - bmr;
	}

	static Double calculateFixedTef(final TefBase base, final Double fixedPercent, final Double bmr,
			final Double getKcal) {
		final TefBase resolvedBase = base != null ? base : TefBase.GET;
		final double percent = fixedPercent != null && fixedPercent > 0 ? fixedPercent : DEFAULT_FIXED_TEF_PERCENT;
		final Double tefBase = resolvedBase == TefBase.BMR ? bmr : getKcal;
		if (tefBase == null || tefBase <= 0) {
			return null;
		}
		final double tef = tefBase * (percent / 100.0);
		log.debug("Calculated fixed TEF: {} kcal/day (base={}, percent={}%)", tef, tefBase, percent);
		return tef;
	}

	static Double calculateMacronutrientTef(final Double getKcal, final Double proteinPercent,
			final Double carbsPercent, final Double fatPercent) {
		if (getKcal == null || getKcal <= 0) {
			return null;
		}
		final MacroDistribution distribution = resolveMacroDistribution(proteinPercent, carbsPercent, fatPercent);
		if (distribution == null) {
			return null;
		}
		final double weightedCoefficient = (distribution.protein() / 100.0) * PROTEIN_TEF_COEFFICIENT
				+ (distribution.carbs() / 100.0) * CARBOHYDRATE_TEF_COEFFICIENT
				+ (distribution.fat() / 100.0) * FAT_TEF_COEFFICIENT;
		final double tef = getKcal * weightedCoefficient;
		log.debug("Calculated macronutrient TEF: {} kcal/day (GET={}, P={}%, C={}%, F={}%)", tef, getKcal,
				distribution.protein(), distribution.carbs(), distribution.fat());
		return tef;
	}

	private static MacroDistribution resolveMacroDistribution(final Double proteinPercent, final Double carbsPercent,
			final Double fatPercent) {
		if (proteinPercent != null && carbsPercent != null && fatPercent != null && proteinPercent >= 0
				&& carbsPercent >= 0 && fatPercent >= 0) {
			final double sum = proteinPercent + carbsPercent + fatPercent;
			if (sum > 0) {
				return new MacroDistribution(proteinPercent, carbsPercent, fatPercent);
			}
		}
		// Balanced default when macros are not configured
		return new MacroDistribution(30.0, 50.0, 20.0);
	}

	private record MacroDistribution(double protein, double carbs, double fat) {
	}

}
