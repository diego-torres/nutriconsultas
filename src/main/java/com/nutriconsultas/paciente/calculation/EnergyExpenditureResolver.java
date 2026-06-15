package com.nutriconsultas.paciente.calculation;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.lang.Nullable;

import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.clinical.exam.AnthropometricMeasurement;
import com.nutriconsultas.paciente.Paciente;

/**
 * Resolves and computes energy expenditure values for patient records.
 */
public final class EnergyExpenditureResolver {

	private EnergyExpenditureResolver() {
		// Utility class
	}

	public record EnergyResult(Double bmr, Double activityFactor, Double getKcal, Double activityKcal, Double tefKcal,
			Double totalAdjustedKcal, Double stressKcal, Double finalTotalKcal) {

		public EnergyResult(final Double bmr, final Double activityFactor, final Double getKcal) {
			this(bmr, activityFactor, getKcal, null, null, getKcal, null, getKcal);
		}

		public EnergyResult(final Double bmr, final Double activityFactor, final Double getKcal,
				final Double activityKcal, final Double tefKcal, final Double totalAdjustedKcal) {
			this(bmr, activityFactor, getKcal, activityKcal, tefKcal, totalAdjustedKcal, null, totalAdjustedKcal);
		}

	}

	/**
	 * Computes BMR, activity factor, GET, TEF and total adjusted energy from patient
	 * context.
	 */
	public static EnergyResult resolve(final Paciente paciente, @Nullable final BmrFormulaType bmrFormulaOverride,
			@Nullable final PhysicalActivityLevel activityLevel, @Nullable final Double customFactorValue,
			final Double weight, final Double height) {
		return resolve(paciente, bmrFormulaOverride, activityLevel, customFactorValue, weight, height, null, null,
				null);
	}

	/**
	 * Computes full energy breakdown including optional physiological stress.
	 */
	public static EnergyResult resolve(final Paciente paciente, @Nullable final BmrFormulaType bmrFormulaOverride,
			@Nullable final PhysicalActivityLevel activityLevel, @Nullable final Double customFactorValue,
			final Double weight, final Double height, @Nullable final CalendarEvent event,
			@Nullable final AnthropometricMeasurement measurement, @Nullable final Double bodyTemperature) {
		if (paciente == null || weight == null || height == null || weight <= 0 || height <= 0) {
			return new EnergyResult(null, null, null, null, null, null, null, null);
		}
		final Integer age = calculateAge(paciente.getDob());
		final Boolean isMale = "M".equalsIgnoreCase(paciente.getGender());
		final BmrFormulaType formula = bmrFormulaOverride != null ? bmrFormulaOverride
				: PatientEnergyPreferences.resolveBmrFormula(paciente);
		final Double bmr = TdeeCalculationService.calculateBmr(formula, weight, height, age, isMale);
		if (bmr == null || activityLevel == null) {
			return new EnergyResult(bmr, null, null, null, null, null, null, null);
		}
		final ActivityFactorScale scale = PatientEnergyPreferences.resolveScale(paciente);
		final CustomActivityFactors customFactors = PatientEnergyPreferences.customFactorsFrom(paciente);
		final Double factor = TdeeCalculationService.resolveActivityFactor(scale, activityLevel, customFactors,
				customFactorValue);
		final Double getKcal = TdeeCalculationService.calculateGet(bmr, factor);
		final EnergyResult withTef = applyTef(paciente, bmr, factor, getKcal);
		return applyStress(paciente, withTef, event, measurement, bodyTemperature);
	}

	/**
	 * Applies TEF preferences to an existing GET/BMR result.
	 */
	public static EnergyResult applyTef(final Paciente paciente, final Double bmr, final Double activityFactor,
			final Double getKcal) {
		if (getKcal == null) {
			return new EnergyResult(bmr, activityFactor, null, null, null, null, null, null);
		}
		final Double tefKcal = TefCalculationService.calculateTef(PatientEnergyPreferences.resolveTefMethod(paciente),
				PatientEnergyPreferences.resolveTefBase(paciente), paciente.getTefFixedPercent(),
				paciente.getTefMacroProteinPercent(), paciente.getTefMacroCarbsPercent(),
				paciente.getTefMacroFatPercent(), bmr, getKcal);
		final Double totalAdjustedKcal = TefCalculationService.calculateTotalAdjustedKcal(getKcal, tefKcal);
		final Double activityKcal = TefCalculationService.calculateActivityKcal(bmr, getKcal);
		return new EnergyResult(bmr, activityFactor, getKcal, activityKcal, tefKcal, totalAdjustedKcal, null,
				totalAdjustedKcal);
	}

	/**
	 * Applies physiological stress to an energy result that already includes TEF.
	 */
	public static EnergyResult applyStress(final Paciente paciente, final EnergyResult result,
			@Nullable final CalendarEvent event, @Nullable final AnthropometricMeasurement measurement,
			@Nullable final Double bodyTemperature) {
		if (result == null || result.getKcal() == null) {
			return result;
		}
		final LocalDate referenceDate = resolveReferenceDate(event, measurement);
		final StressContext stressContext = PhysiologicalStressPreferences.resolveEffective(paciente, event,
				measurement, referenceDate);
		final Double stressKcal = PhysiologicalStressCalculationService.calculateStressKcal(stressContext, result.bmr(),
				result.getKcal(), bodyTemperature);
		final Double finalTotalKcal = PhysiologicalStressCalculationService
			.calculateFinalTotalKcal(result.totalAdjustedKcal(), stressKcal);
		return new EnergyResult(result.bmr(), result.activityFactor(), result.getKcal(), result.activityKcal(),
				result.tefKcal(), result.totalAdjustedKcal(), stressKcal, finalTotalKcal);
	}

	/**
	 * Target daily calories for diet assignment (GET + TEF + stress when available).
	 */
	public static Double resolveTargetDailyCalories(final Paciente paciente) {
		if (paciente == null) {
			return null;
		}
		if (paciente.getFinalTotalKcal() != null) {
			return paciente.getFinalTotalKcal();
		}
		if (paciente.getTotalAdjustedKcal() != null) {
			return paciente.getTotalAdjustedKcal();
		}
		return paciente.getGetKcal();
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
		if (result.tefKcal() != null) {
			paciente.setTefKcal(result.tefKcal());
		}
		if (result.totalAdjustedKcal() != null) {
			paciente.setTotalAdjustedKcal(result.totalAdjustedKcal());
		}
		if (result.stressKcal() != null) {
			paciente.setStressKcal(result.stressKcal());
		}
		if (result.finalTotalKcal() != null) {
			paciente.setFinalTotalKcal(result.finalTotalKcal());
		}
		if (activityLevel != null) {
			paciente.setPhysicalActivityLevel(activityLevel);
		}
		if (result.activityFactor() != null) {
			paciente.setActivityFactor(result.activityFactor());
		}
	}

	private static LocalDate resolveReferenceDate(@Nullable final CalendarEvent event,
			@Nullable final AnthropometricMeasurement measurement) {
		if (measurement != null && measurement.getMeasurementDateTime() != null) {
			return measurement.getMeasurementDateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		}
		if (event != null && event.getEventDateTime() != null) {
			return event.getEventDateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		}
		return LocalDate.now();
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
