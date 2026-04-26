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

	/**
	 * Calculates BMR using FAO/WHO/ONU (UNU) formula. This formula uses weight and age
	 * only, with different coefficients based on age groups and gender. Men: Ages 18-30:
	 * BMR = 15.3 × weight(kg) + 679 Ages 30-60: BMR = 11.6 × weight(kg) + 879 Ages 60+:
	 * BMR = 13.5 × weight(kg) + 487 Women: Ages 18-30: BMR = 14.7 × weight(kg) + 496 Ages
	 * 30-60: BMR = 8.7 × weight(kg) + 829 Ages 60+: BMR = 10.5 × weight(kg) + 596
	 * @param weight weight in kilograms
	 * @param age age in years
	 * @param isMale true if male, false if female
	 * @return BMR in kcal/day, or null if inputs are invalid
	 */
	public static Double calculateFaoWhoOnuBmr(final Double weight, final Integer age, final Boolean isMale) {
		if (weight == null || age == null || isMale == null || weight <= 0 || age < 18) {
			log.debug("Invalid inputs for FAO/WHO/ONU BMR: weight={}, age={}, isMale={}", weight, age, isMale);
			return null;
		}
		final double bmr;
		if (isMale) {
			if (age >= 18 && age < 30) {
				bmr = 15.3 * weight + 679;
			}
			else if (age >= 30 && age < 60) {
				bmr = 11.6 * weight + 879;
			}
			else {
				bmr = 13.5 * weight + 487;
			}
		}
		else {
			if (age >= 18 && age < 30) {
				bmr = 14.7 * weight + 496;
			}
			else if (age >= 30 && age < 60) {
				bmr = 8.7 * weight + 829;
			}
			else {
				bmr = 10.5 * weight + 596;
			}
		}
		log.debug("Calculated FAO/WHO/ONU BMR: {} kcal/day for weight={}kg, age={}, isMale={}", bmr, weight, age,
				isMale);
		return bmr;
	}

	/**
	 * Calculates BMR using Valencia formula (developed for Latin American populations).
	 * Men: BMR = 11.0 × weight(kg) + 16.0 × height(cm) - 2.7 × age(years) + 545 Women:
	 * BMR = 8.0 × weight(kg) + 12.0 × height(cm) - 2.5 × age(years) + 447
	 * @param weight weight in kilograms
	 * @param height height in meters (will be converted to cm)
	 * @param age age in years
	 * @param isMale true if male, false if female
	 * @return BMR in kcal/day, or null if inputs are invalid
	 */
	public static Double calculateValenciaBmr(final Double weight, final Double height, final Integer age,
			final Boolean isMale) {
		if (weight == null || height == null || age == null || isMale == null || weight <= 0 || height <= 0
				|| age <= 0) {
			log.debug("Invalid inputs for Valencia BMR: weight={}, height={}, age={}, isMale={}", weight, height, age,
					isMale);
			return null;
		}
		final double heightCm = height * 100;
		final double bmr;
		if (isMale) {
			bmr = 11.0 * weight + 16.0 * heightCm - 2.7 * age + 545;
		}
		else {
			bmr = 8.0 * weight + 12.0 * heightCm - 2.5 * age + 447;
		}
		log.debug("Calculated Valencia BMR: {} kcal/day for weight={}kg, height={}m, age={}, isMale={}", bmr, weight,
				height, age, isMale);
		return bmr;
	}

	/**
	 * Calculates BMR as the average (promedio) of multiple formulas: Mifflin-St Jeor,
	 * Harris-Benedict, FAO/WHO/ONU, and Valencia. This provides a more balanced estimate
	 * by combining different calculation methods.
	 * @param weight weight in kilograms
	 * @param height height in meters (will be converted to cm)
	 * @param age age in years
	 * @param isMale true if male, false if female
	 * @return Average BMR in kcal/day, or null if inputs are invalid or if any required
	 * formula cannot be calculated
	 */
	public static Double calculatePromedioBmr(final Double weight, final Double height, final Integer age,
			final Boolean isMale) {
		if (weight == null || height == null || age == null || isMale == null || weight <= 0 || height <= 0
				|| age <= 0) {
			log.debug("Invalid inputs for Promedio BMR: weight={}, height={}, age={}, isMale={}", weight, height, age,
					isMale);
			return null;
		}
		final Double mifflinStJeor = calculateMifflinStJeorBmr(weight, height, age, isMale);
		final Double harrisBenedict = calculateHarrisBenedictBmr(weight, height, age, isMale);
		final Double faoWhoOnu = calculateFaoWhoOnuBmr(weight, age, isMale);
		final Double valencia = calculateValenciaBmr(weight, height, age, isMale);
		int validCount = 0;
		double sum = 0.0;
		if (mifflinStJeor != null) {
			sum += mifflinStJeor;
			validCount++;
		}
		if (harrisBenedict != null) {
			sum += harrisBenedict;
			validCount++;
		}
		if (faoWhoOnu != null) {
			sum += faoWhoOnu;
			validCount++;
		}
		if (valencia != null) {
			sum += valencia;
			validCount++;
		}
		if (validCount == 0) {
			log.debug("No valid BMR formulas could be calculated for Promedio BMR");
			return null;
		}
		final double promedio = sum / validCount;
		log.debug(
				"Calculated Promedio BMR: {} kcal/day (average of {} formulas) for weight={}kg, height={}m, age={}, isMale={}",
				promedio, validCount, weight, height, age, isMale);
		return promedio;
	}

}
