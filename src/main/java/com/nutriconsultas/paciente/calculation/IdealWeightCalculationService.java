package com.nutriconsultas.paciente.calculation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

/**
 * Service for calculating ideal/theoretical weight (peso teórico) using various formulas.
 */
@Service
@Slf4j
public final class IdealWeightCalculationService {

	private static final double INCHES_PER_METER = 39.3701;

	private IdealWeightCalculationService() {
		// Utility class - prevent instantiation
	}

	/**
	 * Calculates ideal weight using Robinson formula (1983). Men: Ideal Weight (kg) = 52
	 * + 1.9 × (Height (inches) - 60) Women: Ideal Weight (kg) = 49 + 1.7 × (Height
	 * (inches) - 60)
	 * @param height height in meters (will be converted to inches)
	 * @param isMale true if male, false if female
	 * @return Ideal weight in kg, or null if inputs are invalid
	 */
	public static Double calculateRobinsonIdealWeight(final Double height, final Boolean isMale) {
		if (height == null || isMale == null || height <= 0) {
			log.debug("Invalid inputs for Robinson ideal weight: height={}, isMale={}", height, isMale);
			return null;
		}
		final double heightInches = height * INCHES_PER_METER;
		final double idealWeight;
		if (isMale) {
			idealWeight = 52 + 1.9 * (heightInches - 60);
		}
		else {
			idealWeight = 49 + 1.7 * (heightInches - 60);
		}
		log.debug("Calculated Robinson ideal weight: {} kg for height={}m, isMale={}", idealWeight, height, isMale);
		return idealWeight;
	}

	/**
	 * Calculates ideal weight using Metropolitan (Devine 1974) formula. Men: Ideal Weight
	 * (kg) = 50.0 + 2.3 × (Height (inches) - 60) Women: Ideal Weight (kg) = 45.5 + 2.3 ×
	 * (Height (inches) - 60)
	 * @param height height in meters (will be converted to inches)
	 * @param isMale true if male, false if female
	 * @return Ideal weight in kg, or null if inputs are invalid
	 */
	public static Double calculateMetropolitanIdealWeight(final Double height, final Boolean isMale) {
		if (height == null || isMale == null || height <= 0) {
			log.debug("Invalid inputs for Metropolitan ideal weight: height={}, isMale={}", height, isMale);
			return null;
		}
		final double heightInches = height * INCHES_PER_METER;
		final double idealWeight;
		if (isMale) {
			idealWeight = 50.0 + 2.3 * (heightInches - 60);
		}
		else {
			idealWeight = 45.5 + 2.3 * (heightInches - 60);
		}
		log.debug("Calculated Metropolitan ideal weight: {} kg for height={}m, isMale={}", idealWeight, height, isMale);
		return idealWeight;
	}

	/**
	 * Calculates ideal weight using Hamwi formula (1964). Men: Ideal Weight (kg) = 48.0 +
	 * 2.7 × (Height (inches) - 60) Women: Ideal Weight (kg) = 45.5 + 2.2 × (Height
	 * (inches) - 60)
	 * @param height height in meters (will be converted to inches)
	 * @param isMale true if male, false if female
	 * @return Ideal weight in kg, or null if inputs are invalid
	 */
	public static Double calculateHamwiIdealWeight(final Double height, final Boolean isMale) {
		if (height == null || isMale == null || height <= 0) {
			log.debug("Invalid inputs for Hamwi ideal weight: height={}, isMale={}", height, isMale);
			return null;
		}
		final double heightInches = height * INCHES_PER_METER;
		final double idealWeight;
		if (isMale) {
			idealWeight = 48.0 + 2.7 * (heightInches - 60);
		}
		else {
			idealWeight = 45.5 + 2.2 * (heightInches - 60);
		}
		log.debug("Calculated Hamwi ideal weight: {} kg for height={}m, isMale={}", idealWeight, height, isMale);
		return idealWeight;
	}

	/**
	 * Calculates ideal weight using Lorentz formula (1929). Men: Ideal Weight (kg) =
	 * (Height (cm) - 100) - ((Height (cm) - 150) / 4) Women: Ideal Weight (kg) = (Height
	 * (cm) - 100) - ((Height (cm) - 150) / 2)
	 * @param height height in meters (will be converted to cm)
	 * @param isMale true if male, false if female
	 * @return Ideal weight in kg, or null if inputs are invalid
	 */
	public static Double calculateLorentzIdealWeight(final Double height, final Boolean isMale) {
		if (height == null || isMale == null || height <= 0) {
			log.debug("Invalid inputs for Lorentz ideal weight: height={}, isMale={}", height, isMale);
			return null;
		}
		final double heightCm = height * 100;
		final double idealWeight;
		if (isMale) {
			idealWeight = (heightCm - 100) - ((heightCm - 150) / 4);
		}
		else {
			idealWeight = (heightCm - 100) - ((heightCm - 150) / 2);
		}
		log.debug("Calculated Lorentz ideal weight: {} kg for height={}m, isMale={}", idealWeight, height, isMale);
		return idealWeight;
	}

	/**
	 * Calculates ideal weight using Traditional (Broca) formula (1871). Ideal Weight (kg)
	 * = Height (cm) - 100
	 * @param height height in meters (will be converted to cm)
	 * @return Ideal weight in kg, or null if input is invalid
	 */
	public static Double calculateTraditionalIdealWeight(final Double height) {
		if (height == null || height <= 0) {
			log.debug("Invalid input for Traditional ideal weight: height={}", height);
			return null;
		}
		final double heightCm = height * 100;
		final double idealWeight = heightCm - 100;
		log.debug("Calculated Traditional ideal weight: {} kg for height={}m", idealWeight, height);
		return idealWeight;
	}

}
