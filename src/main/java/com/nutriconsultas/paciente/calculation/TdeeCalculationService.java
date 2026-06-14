package com.nutriconsultas.paciente.calculation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

/**
 * Service for Total Daily Energy Expenditure (GET / TDEE) calculations.
 *
 * <p>
 * <strong>Formula:</strong> GET (kcal/día) = TMB (BMR) × factor de actividad física
 *
 * <p>
 * <strong>Activity factor scales (bibliography):</strong>
 * <ul>
 * <li><strong>Harris-Benedict:</strong> Revised Harris-Benedict (1990) activity
 * multipliers: 1.2 / 1.375 / 1.55 / 1.725 / 1.9 — standard in clinical nutrition
 * references.</li>
 * <li><strong>FAO/WHO:</strong> FAO/WHO/UNU Expert Consultation on Energy and Protein
 * Requirements (1985) PAL values for adults: ~1.53 / 1.76 / 2.10 / 2.40 / 2.80.</li>
 * <li><strong>OMS/WHO:</strong> WHO Technical Report Series 916 (2004) adult PAL
 * categories: 1.40 / 1.60 / 1.80 / 2.00 / 2.20.</li>
 * <li><strong>Personalizado:</strong> Factors defined per patient by the
 * nutritionist.</li>
 * </ul>
 */
@Service
@Slf4j
public final class TdeeCalculationService {

	private TdeeCalculationService() {
		// Utility class - prevent instantiation
	}

	/**
	 * Calculates GET (TDEE) from BMR and activity factor.
	 * @param bmr basal metabolic rate in kcal/day
	 * @param activityFactor physical activity multiplier
	 * @return GET in kcal/day, or null if inputs are invalid
	 */
	public static Double calculateGet(final Double bmr, final Double activityFactor) {
		if (bmr == null || activityFactor == null || bmr <= 0 || activityFactor <= 0) {
			log.debug("Invalid inputs for GET: bmr={}, activityFactor={}", bmr, activityFactor);
			return null;
		}
		final double get = bmr * activityFactor;
		log.debug("Calculated GET: {} kcal/day (bmr={} × factor={})", get, bmr, activityFactor);
		return get;
	}

	/**
	 * Resolves the activity factor for a level within the given scale.
	 * @param scale activity factor scale
	 * @param level physical activity level
	 * @param customFactors per-patient custom factors (required when scale is CUSTOM)
	 * @param customFactorValue explicit factor when level is
	 * {@link PhysicalActivityLevel#CUSTOM}
	 * @return activity factor, or null if it cannot be resolved
	 */
	public static Double resolveActivityFactor(final ActivityFactorScale scale, final PhysicalActivityLevel level,
			final CustomActivityFactors customFactors, final Double customFactorValue) {
		if (level == null) {
			return null;
		}
		if (level == PhysicalActivityLevel.CUSTOM) {
			if (customFactorValue == null || customFactorValue <= 0) {
				return null;
			}
			return customFactorValue;
		}
		final ActivityFactorScale resolvedScale = scale != null ? scale : ActivityFactorScale.HARRIS_BENEDICT;
		if (resolvedScale == ActivityFactorScale.CUSTOM) {
			if (customFactors == null) {
				return null;
			}
			return customFactors.forLevel(level);
		}
		return standardFactor(resolvedScale, level);
	}

	/**
	 * Calculates BMR using the selected formula.
	 * @param formula BMR formula type
	 * @param weight weight in kg
	 * @param height height in meters
	 * @param age age in years
	 * @param isMale true if male
	 * @return BMR in kcal/day, or null if inputs are invalid
	 */
	public static Double calculateBmr(final BmrFormulaType formula, final Double weight, final Double height,
			final Integer age, final Boolean isMale) {
		if (formula == null) {
			return BmrCalculationService.calculatePromedioBmr(weight, height, age, isMale);
		}
		return switch (formula) {
			case MIFFLIN_ST_JEOR -> BmrCalculationService.calculateMifflinStJeorBmr(weight, height, age, isMale);
			case HARRIS_BENEDICT -> BmrCalculationService.calculateHarrisBenedictBmr(weight, height, age, isMale);
			case FAO_WHO_ONU -> BmrCalculationService.calculateFaoWhoOnuBmr(weight, age, isMale);
			case VALENCIA -> BmrCalculationService.calculateValenciaBmr(weight, height, age, isMale);
			case PROMEDIO -> BmrCalculationService.calculatePromedioBmr(weight, height, age, isMale);
		};
	}

	/**
	 * Calculates GET from anthropometric inputs, BMR formula, activity scale and level.
	 */
	public static Double calculateGetFromInputs(final BmrFormulaType bmrFormula, final Double weight,
			final Double height, final Integer age, final Boolean isMale, final ActivityFactorScale scale,
			final PhysicalActivityLevel level, final CustomActivityFactors customFactors,
			final Double customFactorValue) {
		final Double bmr = calculateBmr(bmrFormula, weight, height, age, isMale);
		if (bmr == null) {
			return null;
		}
		final Double factor = resolveActivityFactor(scale, level, customFactors, customFactorValue);
		return calculateGet(bmr, factor);
	}

	private static Double standardFactor(final ActivityFactorScale scale, final PhysicalActivityLevel level) {
		return switch (scale) {
			case HARRIS_BENEDICT -> harrisBenedictFactor(level);
			case FAO_WHO -> faoWhoFactor(level);
			case OMS -> omsFactor(level);
			case CUSTOM -> null;
		};
	}

	private static Double harrisBenedictFactor(final PhysicalActivityLevel level) {
		return switch (level) {
			case SEDENTARY -> 1.2;
			case LIGHT -> 1.375;
			case MODERATE -> 1.55;
			case INTENSE -> 1.725;
			case VERY_INTENSE -> 1.9;
			default -> null;
		};
	}

	private static Double faoWhoFactor(final PhysicalActivityLevel level) {
		return switch (level) {
			case SEDENTARY -> 1.53;
			case LIGHT -> 1.76;
			case MODERATE -> 2.10;
			case INTENSE -> 2.40;
			case VERY_INTENSE -> 2.80;
			default -> null;
		};
	}

	private static Double omsFactor(final PhysicalActivityLevel level) {
		return switch (level) {
			case SEDENTARY -> 1.40;
			case LIGHT -> 1.60;
			case MODERATE -> 1.80;
			case INTENSE -> 2.00;
			case VERY_INTENSE -> 2.20;
			default -> null;
		};
	}

}
