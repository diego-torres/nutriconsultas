package com.nutriconsultas.paciente.calculation;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.lang.Nullable;

import com.nutriconsultas.calendar.CalendarEvent;
import com.nutriconsultas.clinical.exam.AnthropometricMeasurement;
import com.nutriconsultas.paciente.Paciente;

/**
 * Resolves physiological stress preferences from patient records and clinical event overrides.
 */
public final class PhysiologicalStressPreferences {

	private PhysiologicalStressPreferences() {
		// Utility class
	}

	public static StressContext fromPatient(final Paciente paciente) {
		if (paciente == null) {
			return StressContext.inactive();
		}
		return StressContext.fromValues(paciente.getPhysiologicalStressActive(), paciente.getPhysiologicalStressType(),
				paciente.getStressFormulaTable(), paciente.getStressIncrementMode(), paciente.getStressFactorValue(),
				paciente.getStressValidFrom(), paciente.getStressValidUntil(), paciente.getStressFeverTemperature());
	}

	public static StressContext fromCalendarEvent(final CalendarEvent event) {
		if (event == null) {
			return StressContext.inactive();
		}
		return StressContext.fromValues(event.getPhysiologicalStressActive(), event.getPhysiologicalStressType(),
				event.getStressFormulaTable(), event.getStressIncrementMode(), event.getStressFactorValue(),
				event.getStressValidFrom(), event.getStressValidUntil(), event.getStressFeverTemperature());
	}

	public static StressContext fromMeasurement(final AnthropometricMeasurement measurement) {
		if (measurement == null) {
			return StressContext.inactive();
		}
		return StressContext.fromValues(measurement.getPhysiologicalStressActive(),
				measurement.getPhysiologicalStressType(),
				measurement.getStressFormulaTable(), measurement.getStressIncrementMode(),
				measurement.getStressFactorValue(), measurement.getStressValidFrom(),
				measurement.getStressValidUntil(), measurement.getStressFeverTemperature());
	}

	/**
	 * Resolves effective stress context: event/measurement override when explicitly active,
	 * otherwise patient baseline when valid on the reference date.
	 */
	public static StressContext resolveEffective(final Paciente paciente, @Nullable final CalendarEvent event,
			@Nullable final AnthropometricMeasurement measurement, @Nullable final LocalDate referenceDate) {
		final LocalDate effectiveDate = referenceDate != null ? referenceDate : LocalDate.now();
		if (measurement != null && Boolean.TRUE.equals(measurement.getPhysiologicalStressActive())) {
			final StressContext context = fromMeasurement(measurement);
			if (isActive(context, effectiveDate)) {
				return context;
			}
		}
		if (event != null && Boolean.TRUE.equals(event.getPhysiologicalStressActive())) {
			final StressContext context = fromCalendarEvent(event);
			if (isActive(context, effectiveDate)) {
				return context;
			}
		}
		final StressContext patientContext = fromPatient(paciente);
		if (isActive(patientContext, effectiveDate)) {
			return patientContext;
		}
		return StressContext.inactive();
	}

	public static boolean isActive(final StressContext context, final LocalDate referenceDate) {
		if (context == null || !Boolean.TRUE.equals(context.active())) {
			return false;
		}
		if (context.stressType() == null || context.stressType() == PhysiologicalStressType.NONE) {
			return false;
		}
		final LocalDate validFrom = toLocalDate(context.validFrom());
		final LocalDate validUntil = toLocalDate(context.validUntil());
		if (validFrom != null && referenceDate.isBefore(validFrom)) {
			return false;
		}
		return validUntil == null || !referenceDate.isAfter(validUntil);
	}

	private static LocalDate toLocalDate(final Date date) {
		if (date == null) {
			return null;
		}
		final Date utilDate = date instanceof java.sql.Date ? new Date(date.getTime()) : date;
		return utilDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	}

}
