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
 * Bone mass: bioimpedance over manual {@code masaOseaKg} / {@code porcentajeMasaOsea}.
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
		final Double resolvedFat = resolveFatPercentage(measurement, paciente, imc, clinicalOverride);

		consolidateBoneMass(measurement);

		if (resolvedFat != null) {
			applyMusclePercentageIfAbsent(measurement, resolvedFat);
		}
	}

	private Double resolveFatPercentage(final AnthropometricMeasurement measurement, final Paciente paciente,
			final Double imc, final MetodoObtencionComposicionCorporal clinicalOverride) {
		if (measurement.getPorcentajeGrasaCorporal() != null) {
			syncFatFields(measurement, measurement.getPorcentajeGrasaCorporal());
			assignMethodIfAllowed(measurement, MetodoObtencionComposicionCorporal.MANUAL, clinicalOverride);
			return measurement.getPorcentajeGrasaCorporal();
		}

		if (measurement.getIndiceGrasaCorporal() != null) {
			syncFatFields(measurement, measurement.getIndiceGrasaCorporal());
			assignMethodIfAllowed(measurement, MetodoObtencionComposicionCorporal.MANUAL, clinicalOverride);
			return measurement.getIndiceGrasaCorporal();
		}

		final ResolvedFatPercentage resolvedFat = resolveCalculatedFatPercentage(measurement, paciente, imc);
		if (resolvedFat.fatPercentage() != null) {
			syncFatFields(measurement, resolvedFat.fatPercentage());
			assignMethodIfAllowed(measurement, resolvedFat.method(), clinicalOverride);
			return resolvedFat.fatPercentage();
		}

		return null;
	}

	private void consolidateBoneMass(final AnthropometricMeasurement measurement) {
		final Double weight = measurement.getPeso();
		final ResolvedBoneMass resolved = resolveBoneMass(measurement, weight);
		if (resolved == null) {
			return;
		}
		measurement.setMasaOseaKg(resolved.masaOseaKg());
		measurement.setPorcentajeMasaOsea(resolved.porcentajeMasaOsea());
	}

	private ResolvedBoneMass resolveBoneMass(final AnthropometricMeasurement measurement, final Double weight) {
		final Bioimpedance bioimpedance = measurement.getBioimpedance();
		if (bioimpedance != null) {
			final ResolvedBoneMass fromBio = deriveBoneMass(bioimpedance.getBoneMass(),
					bioimpedance.getBoneMassPercentage(), weight);
			if (fromBio != null) {
				log.debug("Using bioimpedance bone mass for measurement");
				return fromBio;
			}
		}

		return deriveBoneMass(measurement.getMasaOseaKg(), measurement.getPorcentajeMasaOsea(), weight);
	}

	private ResolvedBoneMass deriveBoneMass(final Double masaOseaKg, final Double porcentajeMasaOsea,
			final Double weight) {
		if (masaOseaKg != null && porcentajeMasaOsea != null) {
			return new ResolvedBoneMass(masaOseaKg, porcentajeMasaOsea);
		}
		if (masaOseaKg != null && weight != null && weight > 0) {
			final double percentage = (masaOseaKg / weight) * 100.0;
			return new ResolvedBoneMass(masaOseaKg, percentage);
		}
		if (porcentajeMasaOsea != null && weight != null && weight > 0) {
			final double kg = (porcentajeMasaOsea / 100.0) * weight;
			return new ResolvedBoneMass(kg, porcentajeMasaOsea);
		}
		if (masaOseaKg != null) {
			return new ResolvedBoneMass(masaOseaKg, null);
		}
		if (porcentajeMasaOsea != null) {
			return new ResolvedBoneMass(null, porcentajeMasaOsea);
		}
		return null;
	}

	private Double resolveWaterPercentage(final AnthropometricMeasurement measurement) {
		if (measurement.getBioimpedance() == null) {
			return null;
		}
		return measurement.getBioimpedance().getTotalBodyWaterPercentage();
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
		final Double bonePercentage = measurement.getPorcentajeMasaOsea();
		final Double waterPercentage = resolveWaterPercentage(measurement);
		double muscleMassPercentage;
		if (bonePercentage != null && waterPercentage != null) {
			muscleMassPercentage = 100.0 - fatPercentage - bonePercentage - waterPercentage;
		}
		else if (bonePercentage != null) {
			muscleMassPercentage = 100.0 - fatPercentage - bonePercentage;
		}
		else {
			muscleMassPercentage = 100.0 - fatPercentage;
		}
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

	private record ResolvedBoneMass(Double masaOseaKg, Double porcentajeMasaOsea) {
	}

}
