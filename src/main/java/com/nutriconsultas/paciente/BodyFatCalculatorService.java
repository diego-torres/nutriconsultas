package com.nutriconsultas.paciente;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for calculating body fat index/percentage using various methods.
 */
@Service
@Slf4j
public class BodyFatCalculatorService {

	/**
	 * Calculates body fat percentage using the Deurenberg formula. This formula uses BMI,
	 * age, and gender.
	 * @param bmi Body Mass Index
	 * @param age Age in years
	 * @param gender Gender ('M' for male, 'F' for female)
	 * @return Body fat percentage (0-100)
	 */
	public Double calculateBodyFatPercentage(final Double bmi, final Integer age, final String gender) {
		if (bmi == null || age == null || gender == null) {
			log.warn("Cannot calculate body fat: missing required parameters (bmi: {}, age: {}, gender: {})", bmi, age,
					gender);
			return null;
		}
		if (bmi <= 0 || age <= 0) {
			log.warn("Invalid parameters for body fat calculation (bmi: {}, age: {})", bmi, age);
			return null;
		}
		// Deurenberg formula: BF% = (1.20 × BMI) + (0.23 × Age) - (10.8 × gender) - 5.4
		// gender: 1 for male, 0 for female
		final int genderFactor = "M".equalsIgnoreCase(gender) ? 1 : 0;
		final double bodyFatPercentage = (1.20 * bmi) + (0.23 * age) - (10.8 * genderFactor) - 5.4;
		// Clamp to reasonable range (5% to 50%)
		final double clamped = Math.max(5.0, Math.min(50.0, bodyFatPercentage));
		log.debug("Calculated body fat percentage: {}% (bmi: {}, age: {}, gender: {})", clamped, bmi, age, gender);
		return clamped;
	}

	/**
	 * Calculates body fat percentage using skinfold measurements (Jackson-Pollock 3-site
	 * method). This method requires skinfold measurements at three sites.
	 * @param chestSkinfold Chest skinfold measurement in mm
	 * @param abdominalSkinfold Abdominal skinfold measurement in mm
	 * @param thighSkinfold Thigh skinfold measurement in mm
	 * @param age Age in years
	 * @param gender Gender ('M' for male, 'F' for female)
	 * @return Body fat percentage (0-100)
	 */
	public Double calculateBodyFatFromSkinfolds(final Double chestSkinfold, final Double abdominalSkinfold,
			final Double thighSkinfold, final Integer age, final String gender) {
		if (chestSkinfold == null || abdominalSkinfold == null || thighSkinfold == null || age == null
				|| gender == null) {
			log.warn("Cannot calculate body fat from skinfolds: missing required parameters");
			return null;
		}
		final double sum = chestSkinfold + abdominalSkinfold + thighSkinfold;
		if (sum <= 0 || age <= 0) {
			log.warn("Invalid parameters for skinfold body fat calculation");
			return null;
		}
		// Jackson-Pollock 3-site formula
		double bodyDensity;
		if ("M".equalsIgnoreCase(gender)) {
			// Male formula
			bodyDensity = 1.109_38 - (0.000_826_7 * sum) + (0.000_001_6 * sum * sum) - (0.000_257_4 * age);
		}
		else {
			// Female formula
			bodyDensity = 1.099_492_1 - (0.000_992_9 * sum) + (0.000_002_3 * sum * sum) - (0.000_139_2 * age);
		}
		// Convert body density to body fat percentage using Siri equation
		final double bodyFatPercentage = ((4.95 / bodyDensity) - 4.5) * 100;
		// Clamp to reasonable range
		final double clamped = Math.max(5.0, Math.min(50.0, bodyFatPercentage));
		log.debug("Calculated body fat from skinfolds: {}%", clamped);
		return clamped;
	}

	/**
	 * Calculates body fat percentage using the US Navy method (circumference
	 * measurements).
	 * @param waist Waist circumference in cm
	 * @param neck Neck circumference in cm
	 * @param height Height in cm
	 * @param gender Gender ('M' for male, 'F' for female)
	 * @return Body fat percentage (0-100)
	 */
	public Double calculateBodyFatFromCircumferences(final Double waist, final Double neck, final Double height,
			final String gender) {
		if (waist == null || neck == null || height == null || gender == null) {
			log.warn("Cannot calculate body fat from circumferences: missing required parameters");
			return null;
		}
		if (waist <= 0 || neck <= 0 || height <= 0) {
			log.warn("Invalid parameters for circumference body fat calculation");
			return null;
		}
		double bodyFatPercentage;
		if ("M".equalsIgnoreCase(gender)) {
			// Male formula: BF% = 495 / (1.0324 - 0.19077 * log10(waist - neck) + 0.15456
			// * log10(height)) - 450
			final double logValue = Math.log10(waist - neck);
			final double heightLog = Math.log10(height);
			bodyFatPercentage = 495 / (1.032_4 - 0.190_77 * logValue + 0.154_56 * heightLog) - 450;
		}
		else {
			// Female formula requires hip measurement as well
			// For now, use a simplified version
			final double logValue = Math.log10(waist - neck);
			final double heightLog = Math.log10(height);
			bodyFatPercentage = 495 / (1.295_79 - 0.350_04 * logValue + 0.221_00 * heightLog) - 450;
		}
		// Clamp to reasonable range
		final double clamped = Math.max(5.0, Math.min(50.0, bodyFatPercentage));
		log.debug("Calculated body fat from circumferences: {}%", clamped);
		return clamped;
	}

}
