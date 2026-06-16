package com.nutriconsultas.clinical.exam.anthropometric;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.stereotype.Service;

import com.nutriconsultas.clinical.exam.AnthropometricMeasurement;
import com.nutriconsultas.paciente.BodyFatCalculatorService;
import com.nutriconsultas.paciente.Paciente;

import lombok.extern.slf4j.Slf4j;

/**
 * Consolidates body composition fields on anthropometric measurements.
 *
 * <p>
 * Precedence: manual {@code porcentajeGrasaCorporal} / {@code indiceGrasaCorporal},
 * bioimpedance {@code bodyFatPercentage}, Jackson-Pollock skinfolds, Deurenberg estimate.
 */
@Service
@Slf4j
public class BodyCompositionService {

	private final BodyFatCalculatorService bodyFatCalculatorService;

	public BodyCompositionService(final BodyFatCalculatorService bodyFatCalculatorService) {
		this.bodyFatCalculatorService = bodyFatCalculatorService;
	}

	public void applyToMeasurement(final AnthropometricMeasurement measurement, final Paciente paciente,
			final Double imc) {
		ensureBodyComposition(measurement);
		final MetodoObtencionComposicionCorporal clinicalOverride = resolveClinicalOverride(measurement);

		if (measurement.getPorcentajeGrasaCorporal() != null) {
			syncFatFields(measurement, measurement.getPorcentajeGrasaCorporal());
			applyMusclePercentageIfAbsent(measurement, measurement.getPorcentajeGrasaCorporal());
			assignMethodIfAllowed(measurement, MetodoObtencionComposicionCorporal.MANUAL, clinicalOverride);
			return;
		}

		if (measurement.getIndiceGrasaCorporal() != null) {
			syncFatFields(measurement, measurement.getIndiceGrasaCorporal());
			applyMusclePercentageIfAbsent(measurement, measurement.getIndiceGrasaCorporal());
			assignMethodIfAllowed(measurement, MetodoObtencionComposicionCorporal.MANUAL, clinicalOverride);
			return;
		}

		final ResolvedFatPercentage resolvedFat = resolveCalculatedFatPercentage(measurement, paciente, imc);
		if (resolvedFat.fatPercentage() != null) {
			syncFatFields(measurement, resolvedFat.fatPercentage());
			applyMusclePercentageIfAbsent(measurement, resolvedFat.fatPercentage());
			assignMethodIfAllowed(measurement, resolvedFat.method(), clinicalOverride);
		}
	}

	private MetodoObtencionComposicionCorporal resolveClinicalOverride(final AnthropometricMeasurement measurement) {
		if (measurement.getBodyComposition() == null) {
			return null;
		}
		final MetodoObtencionComposicionCorporal method = measurement.getBodyComposition().getMetodoObtencion();
		if (method != null && method.isClinicalOverride()) {
			return method;
		}
		return null;
	}

	private void assignMethodIfAllowed(final AnthropometricMeasurement measurement,
			final MetodoObtencionComposicionCorporal autoMethod,
			final MetodoObtencionComposicionCorporal clinicalOverride) {
		if (clinicalOverride != null) {
			measurement.getBodyComposition().setMetodoObtencion(clinicalOverride);
			return;
		}
		measurement.getBodyComposition().setMetodoObtencion(autoMethod);
	}

	private ResolvedFatPercentage resolveCalculatedFatPercentage(final AnthropometricMeasurement measurement,
			final Paciente paciente, final Double imc) {
		final Double bioimpedanceFat = resolveBioimpedanceFatPercentage(measurement);
		if (bioimpedanceFat != null) {
			log.debug("Using bioimpedance body fat percentage for measurement");
			return new ResolvedFatPercentage(bioimpedanceFat, MetodoObtencionComposicionCorporal.BIOIMPEDANCIA);
		}

		final Double skinfoldFat = calculateSkinfoldFatPercentage(measurement, paciente);
		if (skinfoldFat != null) {
			log.debug("Using Jackson-Pollock skinfold body fat percentage for measurement");
			return new ResolvedFatPercentage(skinfoldFat, MetodoObtencionComposicionCorporal.PLIEGUES);
		}

		final Double deurenbergFat = calculateDeurenbergFatPercentage(imc, paciente);
		if (deurenbergFat != null) {
			log.debug("Using Deurenberg body fat percentage for measurement");
			return new ResolvedFatPercentage(deurenbergFat, MetodoObtencionComposicionCorporal.DEURENBERG);
		}

		return new ResolvedFatPercentage(null, null);
	}

	private Double resolveBioimpedanceFatPercentage(final AnthropometricMeasurement measurement) {
		if (measurement.getBioimpedance() == null) {
			return null;
		}
		return measurement.getBioimpedance().getBodyFatPercentage();
	}

	private Double calculateSkinfoldFatPercentage(final AnthropometricMeasurement measurement,
			final Paciente paciente) {
		if (measurement.getSkinfolds() == null || paciente.getGender() == null) {
			return null;
		}
		final Skinfolds skinfolds = measurement.getSkinfolds();
		final Double chest = skinfolds.getPectoralSkinfold();
		final Double abdominal = skinfolds.getAbdominalSkinfold();
		final Double thigh = skinfolds.getFrontalThighSkinfold();
		final Integer age = calculateAge(paciente.getDob());
		if (chest == null || abdominal == null || thigh == null || age == null) {
			return null;
		}
		return bodyFatCalculatorService.calculateBodyFatFromSkinfolds(chest, abdominal, thigh, age,
				paciente.getGender());
	}

	private Double calculateDeurenbergFatPercentage(final Double imc, final Paciente paciente) {
		if (imc == null || paciente.getDob() == null || paciente.getGender() == null) {
			return null;
		}
		final Integer age = calculateAge(paciente.getDob());
		if (age == null) {
			return null;
		}
		return bodyFatCalculatorService.calculateBodyFatPercentage(imc, age, paciente.getGender());
	}

	private void syncFatFields(final AnthropometricMeasurement measurement, final Double fatPercentage) {
		measurement.setPorcentajeGrasaCorporal(fatPercentage);
		measurement.setIndiceGrasaCorporal(fatPercentage);
	}

	private void applyMusclePercentageIfAbsent(final AnthropometricMeasurement measurement,
			final Double fatPercentage) {
		if (measurement.getPorcentajeMasaMuscular() != null || fatPercentage == null) {
			return;
		}
		final double muscleMassPercentage = 100.0 - fatPercentage;
		final double clamped = Math.max(20.0, Math.min(80.0, muscleMassPercentage));
		measurement.setPorcentajeMasaMuscular(clamped);
	}

	private void ensureBodyComposition(final AnthropometricMeasurement measurement) {
		if (measurement.getBodyComposition() == null) {
			measurement.setBodyComposition(new BodyComposition());
		}
	}

	private Integer calculateAge(final Date dob) {
		if (dob == null) {
			return null;
		}
		final LocalDate birthDate;
		if (dob instanceof java.sql.Date sqlDate) {
			birthDate = sqlDate.toLocalDate();
		}
		else {
			birthDate = dob.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		}
		final LocalDate currentDate = LocalDate.now();
		if (birthDate.isAfter(currentDate)) {
			return null;
		}
		return Period.between(birthDate, currentDate).getYears();
	}

	private record ResolvedFatPercentage(Double fatPercentage, MetodoObtencionComposicionCorporal method) {
	}

}
