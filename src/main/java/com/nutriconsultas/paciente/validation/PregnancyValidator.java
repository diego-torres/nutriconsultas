package com.nutriconsultas.paciente.validation;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.nutriconsultas.paciente.Paciente;

import lombok.extern.slf4j.Slf4j;

/**
 * Validator for pregnancy state. Ensures that pregnancy can only be set to true for
 * female patients aged 12-50.
 */
@Slf4j
public class PregnancyValidator implements ConstraintValidator<ValidPregnancy, Boolean> {

	@Override
	public void initialize(final ValidPregnancy constraintAnnotation) {
		// No initialization needed
	}

	@Override
	public boolean isValid(final Boolean pregnancy, final ConstraintValidatorContext context) {
		// If pregnancy is null or false, it's valid (no pregnancy)
		// Actual validation for pregnancy eligibility is done in the controller
		// since we need access to the full Paciente object (gender and dob)
		return true;
	}

	/**
	 * Validates pregnancy eligibility for a patient. This method should be called from
	 * the controller or service layer.
	 * @param paciente the patient to validate
	 * @return true if pregnancy can be set, false otherwise
	 */
	public static boolean isEligibleForPregnancy(final Paciente paciente) {
		if (paciente == null) {
			return false;
		}

		// Check gender - must be female
		if (paciente.getGender() == null || !"F".equals(paciente.getGender())) {
			log.debug("Patient is not female, cannot set pregnancy");
			return false;
		}

		// Check age - must be between 12 and 50
		if (paciente.getDob() == null) {
			log.debug("Patient has no date of birth, cannot validate pregnancy eligibility");
			return false;
		}

		final Integer age = calculateAge(paciente.getDob());
		if (age == null) {
			log.debug("Could not calculate age for patient, cannot validate pregnancy eligibility");
			return false;
		}

		final boolean eligible = age >= 12 && age <= 50;
		if (!eligible) {
			log.debug("Patient age {} is not between 12 and 50, cannot set pregnancy", age);
		}
		return eligible;
	}

	/**
	 * Calculates age from date of birth.
	 * @param dob date of birth
	 * @return age in years, or null if dob is null or in the future
	 */
	private static Integer calculateAge(final Date dob) {
		if (dob == null) {
			return null;
		}
		LocalDate birthDate;
		if (dob instanceof java.sql.Date) {
			birthDate = ((java.sql.Date) dob).toLocalDate();
		}
		else {
			birthDate = dob.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		}
		final LocalDate currentDate = LocalDate.now();
		if (birthDate.isAfter(currentDate)) {
			log.warn("Date of birth is in the future: {}", dob);
			return null;
		}
		return currentDate.getYear() - birthDate.getYear()
				- (currentDate.getDayOfYear() < birthDate.getDayOfYear() ? 1 : 0);
	}

}
