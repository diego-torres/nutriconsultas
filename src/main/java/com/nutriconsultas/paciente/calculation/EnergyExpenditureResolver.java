package com.nutriconsultas.paciente.calculation;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.lang.Nullable;

import com.nutriconsultas.paciente.Paciente;

/**
 * Resolves and computes energy expenditure values for patient records.
 */
public final class EnergyExpenditureResolver {

	private EnergyExpenditureResolver() {
		// Utility class
	}

	public record EnergyResult(Double bmr, Double activityFactor, Double getKcal) {
	}

	/**
	 * Computes BMR, activity factor and GET from patient context and inputs.
	 */
	public static EnergyResult resolve(final Paciente paciente, @Nullable final BmrFormulaType bmrFormulaOverride,
			@Nullable final PhysicalActivityLevel activityLevel, @Nullable final Double customFactorValue,
			final Double weight, final Double height) {
		if (paciente == null || weight == null || height == null || weight <= 0 || height <= 0) {
			return new EnergyResult(null, null, null);
		}
		final Integer age = calculateAge(paciente.getDob());
		final Boolean isMale = "M".equalsIgnoreCase(paciente.getGender());
		final BmrFormulaType formula = bmrFormulaOverride != null ? bmrFormulaOverride
				: PatientEnergyPreferences.resolveBmrFormula(paciente);
		final Double bmr = TdeeCalculationService.calculateBmr(formula, weight, height, age, isMale);
		if (bmr == null || activityLevel == null) {
			return new EnergyResult(bmr, null, null);
		}
		final ActivityFactorScale scale = PatientEnergyPreferences.resolveScale(paciente);
		final CustomActivityFactors customFactors = PatientEnergyPreferences.customFactorsFrom(paciente);
		final Double factor = TdeeCalculationService.resolveActivityFactor(scale, activityLevel, customFactors,
				customFactorValue);
		final Double getKcal = TdeeCalculationService.calculateGet(bmr, factor);
		return new EnergyResult(bmr, factor, getKcal);
	}

	/**
	 * Applies computed energy values to the patient snapshot.
	 */
	public static void applyToPatient(final Paciente paciente, final EnergyResult result,
			final PhysicalActivityLevel activityLevel) {
		if (paciente == null || result == null) {
			return;
		}
		if (result.bmr() != null) {
			paciente.setBmr(result.bmr());
		}
		if (result.getKcal() != null) {
			paciente.setGetKcal(result.getKcal());
		}
		if (activityLevel != null) {
			paciente.setPhysicalActivityLevel(activityLevel);
		}
		if (result.activityFactor() != null) {
			paciente.setActivityFactor(result.activityFactor());
		}
	}

	private static Integer calculateAge(final Date dob) {
		if (dob == null) {
			return null;
		}
		final Date utilDate = dob instanceof java.sql.Date ? new Date(dob.getTime()) : dob;
		final LocalDate birthDate = utilDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		final LocalDate currentDate = LocalDate.now();
		if (birthDate.isAfter(currentDate)) {
			return null;
		}
		return Period.between(birthDate, currentDate).getYears();
	}

}
