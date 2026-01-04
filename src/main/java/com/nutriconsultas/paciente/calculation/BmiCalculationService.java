package com.nutriconsultas.paciente.calculation;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

/**
 * Service for calculating BMI (Body Mass Index) using various formulas.
 */
@Service
@Slf4j
public final class BmiCalculationService {

	private BmiCalculationService() {
		// Utility class - prevent instantiation
	}

	/**
	 * Calculates Classic BMI (Quetelet formula): weight / height^2
	 * @param weight weight in kilograms
	 * @param height height in meters
	 * @return BMI value, or null if inputs are invalid
	 */
	public static Double calculateClassicBmi(final Double weight, final Double height) {
		if (weight == null || height == null || weight <= 0 || height <= 0) {
			log.debug("Invalid inputs for Classic BMI: weight={}, height={}", weight, height);
			return null;
		}
		final double bmi = weight / (height * height);
		log.debug("Calculated Classic BMI: {} for weight={}kg, height={}m", bmi, weight, height);
		return bmi;
	}

	/**
	 * Calculates Trefethen corrected BMI: 1.3 * weight / height^2.5
	 * @param weight weight in kilograms
	 * @param height height in meters
	 * @return Trefethen BMI value, or null if inputs are invalid
	 */
	public static Double calculateTrefethenBmi(final Double weight, final Double height) {
		if (weight == null || height == null || weight <= 0 || height <= 0) {
			log.debug("Invalid inputs for Trefethen BMI: weight={}, height={}", weight, height);
			return null;
		}
		final double bmi = 1.3 * weight / Math.pow(height, 2.5);
		log.debug("Calculated Trefethen BMI: {} for weight={}kg, height={}m", bmi, weight, height);
		return bmi;
	}

}
