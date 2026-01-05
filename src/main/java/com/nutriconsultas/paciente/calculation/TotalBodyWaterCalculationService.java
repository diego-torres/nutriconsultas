package com.nutriconsultas.paciente.calculation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

/**
 * Service for calculating Total Body Water (Agua Corporal Total, TBW) using various
 * formulas.
 */
@Service
@Slf4j
public final class TotalBodyWaterCalculationService {

	private TotalBodyWaterCalculationService() {
		// Utility class - prevent instantiation
	}

	/**
	 * Calculates Total Body Water using Watson formula. Men: TBW = 2.447 - (0.09156 ×
	 * age(years)) + (0.1074 × height(cm)) + (0.3362 × weight(kg)) Women: TBW = -2.097 +
	 * (0.1069 × height(cm)) + (0.2466 × weight(kg))
	 * @param weight weight in kilograms
	 * @param height height in meters (will be converted to cm)
	 * @param age age in years (required for men, not used for women)
	 * @param isMale true if male, false if female
	 * @return Total Body Water in liters, or null if inputs are invalid
	 */
	public static Double calculateWatsonTbw(final Double weight, final Double height, final Integer age,
			final Boolean isMale) {
		if (weight == null || height == null || isMale == null || weight <= 0 || height <= 0) {
			log.debug("Invalid inputs for Watson TBW: weight={}, height={}, age={}, isMale={}", weight, height, age,
					isMale);
			return null;
		}
		if (isMale && (age == null || age <= 0)) {
			log.debug("Age is required for Watson TBW calculation in men: age={}", age);
			return null;
		}
		final double heightCm = height * 100;
		final double tbw;
		if (isMale) {
			tbw = 2.447 - (0.09156 * age) + (0.1074 * heightCm) + (0.3362 * weight);
		}
		else {
			tbw = -2.097 + (0.1069 * heightCm) + (0.2466 * weight);
		}
		log.debug("Calculated Watson TBW: {} L for weight={}kg, height={}m, age={}, isMale={}", tbw, weight, height,
				age, isMale);
		return tbw;
	}

	/**
	 * Calculates Total Body Water using Hume-Meyers (Hume-Weyers) formula. Men: TBW =
	 * (0.194786 × height(cm)) + (0.296785 × weight(kg)) - 14.012934 Women: TBW =
	 * (0.344547 × height(cm)) + (0.183809 × weight(kg)) - 35.270121
	 * @param weight weight in kilograms
	 * @param height height in meters (will be converted to cm)
	 * @param isMale true if male, false if female
	 * @return Total Body Water in liters, or null if inputs are invalid
	 */
	public static Double calculateHumeMeyersTbw(final Double weight, final Double height, final Boolean isMale) {
		if (weight == null || height == null || isMale == null || weight <= 0 || height <= 0) {
			log.debug("Invalid inputs for Hume-Meyers TBW: weight={}, height={}, isMale={}", weight, height, isMale);
			return null;
		}
		final double heightCm = height * 100;
		final double tbw;
		if (isMale) {
			tbw = (0.194786 * heightCm) + (0.296785 * weight) - 14.012934;
		}
		else {
			tbw = (0.344547 * heightCm) + (0.183809 * weight) - 35.270121;
		}
		log.debug("Calculated Hume-Meyers TBW: {} L for weight={}kg, height={}m, isMale={}", tbw, weight, height,
				isMale);
		return tbw;
	}

}
