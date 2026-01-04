package com.nutriconsultas.paciente.calculation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

/**
 * Service for calculating BMR (Basal Metabolic Rate) using various formulas.
 */
@Service
@Slf4j
public final class BmrCalculationService {

	private BmrCalculationService() {
		// Utility class - prevent instantiation
	}

	/**
	 * Calculates BMR using Mifflin-St Jeor formula. Men: BMR = 10 * weight(kg) + 6.25 *
	 * height(cm) - 5 * age(years) + 5 Women: BMR = 10 * weight(kg) + 6.25 * height(cm) -
	 * 5 * age(years) - 161
	 * @param weight weight in kilograms
	 * @param height height in meters (will be converted to cm)
	 * @param age age in years
	 * @param isMale true if male, false if female
	 * @return BMR in kcal/day, or null if inputs are invalid
	 */
	public static Double calculateMifflinStJeorBmr(final Double weight, final Double height, final Integer age,
			final Boolean isMale) {
		if (weight == null || height == null || age == null || isMale == null || weight <= 0 || height <= 0
				|| age <= 0) {
			log.debug("Invalid inputs for Mifflin-St Jeor BMR: weight={}, height={}, age={}, isMale={}", weight, height,
					age, isMale);
			return null;
		}
		final double heightCm = height * 100;
		final double bmr = 10 * weight + 6.25 * heightCm - 5 * age + (isMale ? 5 : -161);
		log.debug("Calculated Mifflin-St Jeor BMR: {} kcal/day for weight={}kg, height={}m, age={}, isMale={}", bmr,
				weight, height, age, isMale);
		return bmr;
	}

	/**
	 * Calculates BMR using Harris-Benedict formula (revised 1990). Men: BMR = 13.397 *
	 * weight(kg) + 4.799 * height(cm) - 5.677 * age(years) + 88.362 Women: BMR = 9.247 *
	 * weight(kg) + 3.098 * height(cm) - 4.330 * age(years) + 447.593
	 * @param weight weight in kilograms
	 * @param height height in meters (will be converted to cm)
	 * @param age age in years
	 * @param isMale true if male, false if female
	 * @return BMR in kcal/day, or null if inputs are invalid
	 */
	public static Double calculateHarrisBenedictBmr(final Double weight, final Double height, final Integer age,
			final Boolean isMale) {
		if (weight == null || height == null || age == null || isMale == null || weight <= 0 || height <= 0
				|| age <= 0) {
			log.debug("Invalid inputs for Harris-Benedict BMR: weight={}, height={}, age={}, isMale={}", weight, height,
					age, isMale);
			return null;
		}
		final double heightCm = height * 100;
		final double bmr;
		if (isMale) {
			bmr = 13.397 * weight + 4.799 * heightCm - 5.677 * age + 88.362;
		}
		else {
			bmr = 9.247 * weight + 3.098 * heightCm - 4.330 * age + 447.593;
		}
		log.debug("Calculated Harris-Benedict BMR: {} kcal/day for weight={}kg, height={}m, age={}, isMale={}", bmr,
				weight, height, age, isMale);
		return bmr;
	}

	/**
	 * Calculates BMR using Katch-McArdle formula (requires lean body mass). BMR = 370 +
	 * (21.6 * leanBodyMass(kg))
	 * @param leanBodyMass lean body mass in kilograms
	 * @return BMR in kcal/day, or null if input is invalid
	 */
	public static Double calculateKatchMcArdleBmr(final Double leanBodyMass) {
		if (leanBodyMass == null || leanBodyMass <= 0) {
			log.debug("Invalid input for Katch-McArdle BMR: leanBodyMass={}", leanBodyMass);
			return null;
		}
		final double bmr = 370 + (21.6 * leanBodyMass);
		log.debug("Calculated Katch-McArdle BMR: {} kcal/day for leanBodyMass={}kg", bmr, leanBodyMass);
		return bmr;
	}

	/**
	 * Calculates BMR using Cunningham formula (requires lean body mass). BMR = 500 + (22
	 * * leanBodyMass(kg))
	 * @param leanBodyMass lean body mass in kilograms
	 * @return BMR in kcal/day, or null if input is invalid
	 */
	public static Double calculateCunninghamBmr(final Double leanBodyMass) {
		if (leanBodyMass == null || leanBodyMass <= 0) {
			log.debug("Invalid input for Cunningham BMR: leanBodyMass={}", leanBodyMass);
			return null;
		}
		final double bmr = 500 + (22 * leanBodyMass);
		log.debug("Calculated Cunningham BMR: {} kcal/day for leanBodyMass={}kg", bmr, leanBodyMass);
		return bmr;
	}

}
