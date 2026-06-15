package com.nutriconsultas.paciente.calculation;

import org.springframework.lang.Nullable;

/**
 * Calculates additional energy expenditure from physiological stress.
 */
public final class PhysiologicalStressCalculationService {

	private static final double NORMAL_BODY_TEMPERATURE_CELSIUS = 37.0;

	private PhysiologicalStressCalculationService() {
		// Utility class
	}

	/**
	 * Calculates stress kcal/day from resolved context and energy components.
	 * @param context resolved stress configuration
	 * @param bmr basal metabolic rate (kcal/day)
	 * @param getKcal total daily energy expenditure before TEF (kcal/day)
	 * @param bodyTemperature optional body temperature in °C (for fever formulas)
	 * @return additional kcal/day from stress, or {@code null} when inactive
	 */
	public static Double calculateStressKcal(final StressContext context, @Nullable final Double bmr,
			@Nullable final Double getKcal, @Nullable final Double bodyTemperature) {
		if (context == null || !Boolean.TRUE.equals(context.active()) || context.stressType() == null
				|| context.stressType() == PhysiologicalStressType.NONE) {
			return null;
		}
		final StressFormulaTable formulaTable = context.formulaTable() != null ? context.formulaTable()
				: StressFormulaTable.LONG;
		if (formulaTable == StressFormulaTable.FEVER_PER_DEGREE
				&& context.stressType() == PhysiologicalStressType.FEVER) {
			return calculateFeverStressKcal(bmr, resolveFeverTemperature(context, bodyTemperature));
		}
		final StressIncrementMode mode = context.incrementMode() != null ? context.incrementMode()
				: StressIncrementMode.MULTIPLIER_BMR;
		if (mode == StressIncrementMode.FIXED_KCAL) {
			return resolveFixedKcal(context);
		}
		final Double multiplier = resolveMultiplier(context, formulaTable);
		if (multiplier == null || multiplier <= 1.0) {
			return null;
		}
		return calculateMultiplierStressKcal(mode, multiplier, bmr, getKcal);
	}

	public static Double calculateFinalTotalKcal(final Double totalAdjustedKcal, final Double stressKcal) {
		if (totalAdjustedKcal == null) {
			return null;
		}
		if (stressKcal == null || stressKcal <= 0) {
			return totalAdjustedKcal;
		}
		return totalAdjustedKcal + stressKcal;
	}

	private static Double calculateFeverStressKcal(final Double bmr, @Nullable final Double temperature) {
		if (bmr == null || bmr <= 0 || temperature == null || temperature <= NORMAL_BODY_TEMPERATURE_CELSIUS) {
			return null;
		}
		final double degreesAboveNormal = temperature - NORMAL_BODY_TEMPERATURE_CELSIUS;
		return bmr * PhysiologicalStressCatalog.FEVER_INCREMENT_PER_DEGREE * degreesAboveNormal;
	}

	private static Double resolveFeverTemperature(final StressContext context, @Nullable final Double bodyTemperature) {
		if (bodyTemperature != null) {
			return bodyTemperature;
		}
		return context.feverTemperature();
	}

	private static Double resolveFixedKcal(final StressContext context) {
		if (context.factorValue() == null || context.factorValue() <= 0) {
			return null;
		}
		return context.factorValue();
	}

	private static Double resolveMultiplier(final StressContext context, final StressFormulaTable formulaTable) {
		if (context.factorValue() != null && context.factorValue() > 0) {
			return context.factorValue();
		}
		return PhysiologicalStressCatalog.defaultMultiplier(context.stressType(), formulaTable);
	}

	private static Double calculateMultiplierStressKcal(final StressIncrementMode mode, final Double multiplier,
			@Nullable final Double bmr, @Nullable final Double getKcal) {
		if (mode == StressIncrementMode.MULTIPLIER_GET) {
			if (getKcal == null || getKcal <= 0) {
				return null;
			}
			return getKcal * (multiplier - 1.0);
		}
		if (bmr == null || bmr <= 0) {
			return null;
		}
		return bmr * (multiplier - 1.0);
	}

}
