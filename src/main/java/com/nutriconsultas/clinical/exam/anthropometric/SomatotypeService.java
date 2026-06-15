package com.nutriconsultas.clinical.exam.anthropometric;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.stereotype.Service;

import com.nutriconsultas.clinical.exam.AnthropometricMeasurement;
import com.nutriconsultas.paciente.Paciente;

import lombok.extern.slf4j.Slf4j;

/**
 * Applies Heath-Carter somatotype calculation to anthropometric measurements (adults
 * only).
 */
@Service
@Slf4j
public class SomatotypeService {

	public SomatotypeResult applyToMeasurement(final AnthropometricMeasurement measurement, final Paciente paciente) {
		ensureBodyComposition(measurement);
		final Integer age = calculateAge(paciente != null ? paciente.getDob() : null);
		final SomatotypeMeasurements inputs = SomatotypeMeasurements.builder()
			.weightKg(measurement.getPeso())
			.heightMeters(measurement.getEstatura())
			.tricepsSkinfoldMm(getTricepsSkinfold(measurement))
			.subscapularSkinfoldMm(getSubscapularSkinfold(measurement))
			.supraspinalSkinfoldMm(getSupraspinalSkinfold(measurement))
			.flexedArmGirthCm(getFlexedArmGirth(measurement))
			.calfGirthCm(getCalfGirth(measurement))
			.medialCalfSkinfoldMm(getMedialCalfSkinfold(measurement))
			.humerusBreadthCm(getHumerusBreadth(measurement))
			.femurBreadthCm(getFemurBreadth(measurement))
			.build();
		final SomatotypeResult result = SomatotypeCalculationService.calculate(inputs, age);

		if (result.isCalculable()) {
			persistResult(measurement, result);
			if (log.isDebugEnabled()) {
				log.debug("Somatotype calculated for measurement: endo={}, meso={}, ecto={}", result.getEndomorphy(),
						result.getMesomorphy(), result.getEctomorphy());
			}
		}
		else {
			clearSomatotype(measurement);
		}
		return result;
	}

	private void persistResult(final AnthropometricMeasurement measurement, final SomatotypeResult result) {
		final BodyComposition composition = measurement.getBodyComposition();
		composition.setEndomorphy(result.getEndomorphy());
		composition.setMesomorphy(result.getMesomorphy());
		composition.setEctomorphy(result.getEctomorphy());
		composition.setSomatocartaX(result.getSomatocartaX());
		composition.setSomatocartaY(result.getSomatocartaY());
	}

	private void clearSomatotype(final AnthropometricMeasurement measurement) {
		final BodyComposition composition = measurement.getBodyComposition();
		if (composition == null) {
			return;
		}
		composition.setEndomorphy(null);
		composition.setMesomorphy(null);
		composition.setEctomorphy(null);
		composition.setSomatocartaX(null);
		composition.setSomatocartaY(null);
	}

	private void ensureBodyComposition(final AnthropometricMeasurement measurement) {
		if (measurement.getBodyComposition() == null) {
			measurement.setBodyComposition(new BodyComposition());
		}
	}

	private Double getTricepsSkinfold(final AnthropometricMeasurement measurement) {
		return measurement.getSkinfolds() != null ? measurement.getSkinfolds().getTricepsSkinfold() : null;
	}

	private Double getSubscapularSkinfold(final AnthropometricMeasurement measurement) {
		return measurement.getSkinfolds() != null ? measurement.getSkinfolds().getSubscapularSkinfold() : null;
	}

	private Double getSupraspinalSkinfold(final AnthropometricMeasurement measurement) {
		return measurement.getSkinfolds() != null ? measurement.getSkinfolds().getSupraespinalSkinfold() : null;
	}

	private Double getMedialCalfSkinfold(final AnthropometricMeasurement measurement) {
		return measurement.getSkinfolds() != null ? measurement.getSkinfolds().getMedialCalfSkinfold() : null;
	}

	private Double getFlexedArmGirth(final AnthropometricMeasurement measurement) {
		return measurement.getCircumferences() != null
				? measurement.getCircumferences().getMidUpperArmCircumferenceContracted() : null;
	}

	private Double getCalfGirth(final AnthropometricMeasurement measurement) {
		return measurement.getCircumferences() != null ? measurement.getCircumferences().getCalfCircumference() : null;
	}

	private Double getHumerusBreadth(final AnthropometricMeasurement measurement) {
		return measurement.getDiameters() != null ? measurement.getDiameters().getHumerusDiameter() : null;
	}

	private Double getFemurBreadth(final AnthropometricMeasurement measurement) {
		return measurement.getDiameters() != null ? measurement.getDiameters().getFemurDiameter() : null;
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

}
