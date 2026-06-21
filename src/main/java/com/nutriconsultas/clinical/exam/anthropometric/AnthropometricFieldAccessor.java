package com.nutriconsultas.clinical.exam.anthropometric;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.nutriconsultas.clinical.exam.AnthropometricMeasurement;

/**
 * Reads and writes whitelisted anthropometric field paths (#242).
 */
public final class AnthropometricFieldAccessor {

	private static final Map<String, Function<AnthropometricMeasurement, Double>> READERS = Map.ofEntries(
			Map.entry("bodyMass.weight", m -> m.getBodyMass() != null ? m.getBodyMass().getWeight() : null),
			Map.entry("bodyMass.height", m -> m.getBodyMass() != null ? m.getBodyMass().getHeight() : null),
			Map.entry("skinfolds.subscapularSkinfold",
					m -> m.getSkinfolds() != null ? m.getSkinfolds().getSubscapularSkinfold() : null),
			Map.entry("skinfolds.tricepsSkinfold",
					m -> m.getSkinfolds() != null ? m.getSkinfolds().getTricepsSkinfold() : null),
			Map.entry("skinfolds.bicepsSkinfold",
					m -> m.getSkinfolds() != null ? m.getSkinfolds().getBicepsSkinfold() : null),
			Map.entry("skinfolds.iliacCrestSkinfold",
					m -> m.getSkinfolds() != null ? m.getSkinfolds().getIliacCrestSkinfold() : null),
			Map.entry("skinfolds.supraespinalSkinfold",
					m -> m.getSkinfolds() != null ? m.getSkinfolds().getSupraespinalSkinfold() : null),
			Map.entry("skinfolds.abdominalSkinfold",
					m -> m.getSkinfolds() != null ? m.getSkinfolds().getAbdominalSkinfold() : null),
			Map.entry("skinfolds.frontalThighSkinfold",
					m -> m.getSkinfolds() != null ? m.getSkinfolds().getFrontalThighSkinfold() : null),
			Map.entry("skinfolds.medialCalfSkinfold",
					m -> m.getSkinfolds() != null ? m.getSkinfolds().getMedialCalfSkinfold() : null),
			Map.entry("skinfolds.medialAxillarySkinfold",
					m -> m.getSkinfolds() != null ? m.getSkinfolds().getMedialAxillarySkinfold() : null),
			Map.entry("skinfolds.pectoralSkinfold",
					m -> m.getSkinfolds() != null ? m.getSkinfolds().getPectoralSkinfold() : null),
			Map.entry("circumferences.waistCircumference",
					m -> m.getCircumferences() != null ? m.getCircumferences().getWaistCircumference() : null),
			Map.entry("circumferences.hipCircumference",
					m -> m.getCircumferences() != null ? m.getCircumferences().getHipCircumference() : null),
			Map.entry("circumferences.neckCircumference",
					m -> m.getCircumferences() != null ? m.getCircumferences().getNeckCircumference() : null),
			Map.entry("circumferences.midUpperArmCircumferenceRelaxed",
					m -> m.getCircumferences() != null ? m.getCircumferences().getMidUpperArmCircumferenceRelaxed()
							: null),
			Map.entry("circumferences.midUpperArmCircumferenceContracted",
					m -> m.getCircumferences() != null ? m.getCircumferences().getMidUpperArmCircumferenceContracted()
							: null),
			Map.entry("circumferences.thighCircumference",
					m -> m.getCircumferences() != null ? m.getCircumferences().getThighCircumference() : null),
			Map.entry("circumferences.calfCircumference",
					m -> m.getCircumferences() != null ? m.getCircumferences().getCalfCircumference() : null),
			Map.entry("bioimpedance.bodyFatPercentage",
					m -> m.getBioimpedance() != null ? m.getBioimpedance().getBodyFatPercentage() : null),
			Map.entry("bodyComposition.porcentajeGrasaCorporal", AnthropometricMeasurement::getPorcentajeGrasaCorporal),
			Map.entry("bodyComposition.porcentajeMasaMuscular", AnthropometricMeasurement::getPorcentajeMasaMuscular),
			Map.entry("bodyComposition.masaOseaKg", AnthropometricMeasurement::getMasaOseaKg),
			Map.entry("bodyComposition.porcentajeMasaOsea", AnthropometricMeasurement::getPorcentajeMasaOsea));

	private static final Map<String, BiConsumer<AnthropometricMeasurement, Double>> WRITERS = Map.ofEntries(
			Map.entry("bodyMass.weight", (m, v) -> bodyMass(m).setWeight(v)),
			Map.entry("bodyMass.height", (m, v) -> bodyMass(m).setHeight(v)),
			Map.entry("skinfolds.subscapularSkinfold", (m, v) -> skinfolds(m).setSubscapularSkinfold(v)),
			Map.entry("skinfolds.tricepsSkinfold", (m, v) -> skinfolds(m).setTricepsSkinfold(v)),
			Map.entry("skinfolds.bicepsSkinfold", (m, v) -> skinfolds(m).setBicepsSkinfold(v)),
			Map.entry("skinfolds.iliacCrestSkinfold", (m, v) -> skinfolds(m).setIliacCrestSkinfold(v)),
			Map.entry("skinfolds.supraespinalSkinfold", (m, v) -> skinfolds(m).setSupraespinalSkinfold(v)),
			Map.entry("skinfolds.abdominalSkinfold", (m, v) -> skinfolds(m).setAbdominalSkinfold(v)),
			Map.entry("skinfolds.frontalThighSkinfold", (m, v) -> skinfolds(m).setFrontalThighSkinfold(v)),
			Map.entry("skinfolds.medialCalfSkinfold", (m, v) -> skinfolds(m).setMedialCalfSkinfold(v)),
			Map.entry("skinfolds.medialAxillarySkinfold", (m, v) -> skinfolds(m).setMedialAxillarySkinfold(v)),
			Map.entry("skinfolds.pectoralSkinfold", (m, v) -> skinfolds(m).setPectoralSkinfold(v)),
			Map.entry("circumferences.waistCircumference", (m, v) -> circumferences(m).setWaistCircumference(v)),
			Map.entry("circumferences.hipCircumference", (m, v) -> circumferences(m).setHipCircumference(v)),
			Map.entry("circumferences.neckCircumference", (m, v) -> circumferences(m).setNeckCircumference(v)),
			Map.entry("circumferences.midUpperArmCircumferenceRelaxed",
					(m, v) -> circumferences(m).setMidUpperArmCircumferenceRelaxed(v)),
			Map.entry("circumferences.midUpperArmCircumferenceContracted",
					(m, v) -> circumferences(m).setMidUpperArmCircumferenceContracted(v)),
			Map.entry("circumferences.thighCircumference", (m, v) -> circumferences(m).setThighCircumference(v)),
			Map.entry("circumferences.calfCircumference", (m, v) -> circumferences(m).setCalfCircumference(v)),
			Map.entry("bioimpedance.bodyFatPercentage", (m, v) -> bioimpedance(m).setBodyFatPercentage(v)),
			Map.entry("bodyComposition.porcentajeGrasaCorporal",
					(m, v) -> bodyComposition(m).setPorcentajeGrasaCorporal(v)),
			Map.entry("bodyComposition.porcentajeMasaMuscular",
					(m, v) -> bodyComposition(m).setPorcentajeMasaMuscular(v)),
			Map.entry("bodyComposition.masaOseaKg", (m, v) -> bodyComposition(m).setMasaOseaKg(v)),
			Map.entry("bodyComposition.porcentajeMasaOsea", (m, v) -> bodyComposition(m).setPorcentajeMasaOsea(v)));

	private AnthropometricFieldAccessor() {
	}

	public static Double read(final AnthropometricMeasurement measurement, final String fieldKey) {
		if (measurement == null || fieldKey == null) {
			return null;
		}
		final Function<AnthropometricMeasurement, Double> reader = READERS.get(fieldKey);
		return reader != null ? reader.apply(measurement) : null;
	}

	public static void write(final AnthropometricMeasurement measurement, final String fieldKey, final Double value) {
		final BiConsumer<AnthropometricMeasurement, Double> writer = WRITERS.get(fieldKey);
		if (writer == null) {
			throw new IllegalArgumentException("Campo no editable: " + fieldKey);
		}
		writer.accept(measurement, value);
	}

	private static BodyMass bodyMass(final AnthropometricMeasurement measurement) {
		if (measurement.getBodyMass() == null) {
			measurement.setBodyMass(new BodyMass());
		}
		return measurement.getBodyMass();
	}

	private static Skinfolds skinfolds(final AnthropometricMeasurement measurement) {
		if (measurement.getSkinfolds() == null) {
			measurement.setSkinfolds(new Skinfolds());
		}
		return measurement.getSkinfolds();
	}

	private static Circumferences circumferences(final AnthropometricMeasurement measurement) {
		if (measurement.getCircumferences() == null) {
			measurement.setCircumferences(new Circumferences());
		}
		return measurement.getCircumferences();
	}

	private static Bioimpedance bioimpedance(final AnthropometricMeasurement measurement) {
		if (measurement.getBioimpedance() == null) {
			measurement.setBioimpedance(new Bioimpedance());
		}
		return measurement.getBioimpedance();
	}

	private static BodyComposition bodyComposition(final AnthropometricMeasurement measurement) {
		if (measurement.getBodyComposition() == null) {
			measurement.setBodyComposition(new BodyComposition());
		}
		return measurement.getBodyComposition();
	}

}
