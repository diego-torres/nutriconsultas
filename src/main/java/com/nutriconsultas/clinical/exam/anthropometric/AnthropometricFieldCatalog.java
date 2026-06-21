package com.nutriconsultas.clinical.exam.anthropometric;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.nutriconsultas.clinical.exam.AnthropometricMeasurement;

/**
 * Whitelist of per-field corrections on anthropometric measurements (#242).
 */
public final class AnthropometricFieldCatalog {

	private static final List<AnthropometricFieldDefinition> DEFINITIONS = List.of(
			field("bodyMass.weight", "Peso", "kg", 10.0, 200.0,
					Set.of(AnthropometricRecalcGroup.BMI, AnthropometricRecalcGroup.COMPOSITION,
							AnthropometricRecalcGroup.ENERGY, AnthropometricRecalcGroup.PATIENT_SNAPSHOT),
					true),
			field("bodyMass.height", "Estatura", "m", 0.5, 3.0,
					Set.of(AnthropometricRecalcGroup.BMI, AnthropometricRecalcGroup.COMPOSITION,
							AnthropometricRecalcGroup.ENERGY, AnthropometricRecalcGroup.PATIENT_SNAPSHOT),
					true),
			skinfold("skinfolds.subscapularSkinfold", "Subescapular", 1.0, 80.0),
			skinfold("skinfolds.tricepsSkinfold", "Tríceps", 1.0, 80.0),
			skinfold("skinfolds.bicepsSkinfold", "Bíceps", 1.0, 80.0),
			skinfold("skinfolds.iliacCrestSkinfold", "Cresta iliaca", 1.0, 80.0),
			skinfold("skinfolds.supraespinalSkinfold", "Supraespinal", 1.0, 80.0),
			skinfold("skinfolds.abdominalSkinfold", "Abdominal", 1.0, 80.0),
			skinfold("skinfolds.frontalThighSkinfold", "Muslo frontal", 1.0, 80.0),
			skinfold("skinfolds.medialCalfSkinfold", "Pantorrilla medial", 1.0, 80.0),
			skinfold("skinfolds.medialAxillarySkinfold", "Axilar medial", 1.0, 80.0),
			skinfold("skinfolds.pectoralSkinfold", "Pectoral", 1.0, 80.0),
			circumference("circumferences.waistCircumference", "Cintura", 20.0, 250.0),
			circumference("circumferences.hipCircumference", "Cadera", 20.0, 250.0),
			circumference("circumferences.neckCircumference", "Cuello", 20.0, 80.0),
			circumference("circumferences.midUpperArmCircumferenceRelaxed", "Brazo relajado", 10.0, 80.0),
			circumference("circumferences.midUpperArmCircumferenceContracted", "Brazo contraído", 10.0, 90.0),
			circumference("circumferences.thighCircumference", "Muslo", 20.0, 120.0),
			circumference("circumferences.calfCircumference", "Pantorrilla", 15.0, 80.0),
			field("bioimpedance.bodyFatPercentage", "% grasa (bioimpedancia)", "%", 3.0, 65.0,
					Set.of(AnthropometricRecalcGroup.COMPOSITION), true),
			field("bodyComposition.porcentajeGrasaCorporal", "% grasa corporal", "%", 3.0, 65.0,
					Set.of(AnthropometricRecalcGroup.COMPOSITION), false),
			field("bodyComposition.porcentajeMasaMuscular", "% masa muscular", "%", 10.0, 90.0,
					Set.of(AnthropometricRecalcGroup.COMPOSITION), false),
			field("bodyComposition.masaOseaKg", "Masa ósea", "kg", 0.5, 20.0,
					Set.of(AnthropometricRecalcGroup.COMPOSITION), false),
			field("bodyComposition.porcentajeMasaOsea", "% masa ósea", "%", 1.0, 30.0,
					Set.of(AnthropometricRecalcGroup.COMPOSITION), false));

	private static final Map<String, AnthropometricFieldDefinition> BY_KEY = DEFINITIONS.stream()
		.collect(Collectors.toUnmodifiableMap(AnthropometricFieldDefinition::fieldKey, Function.identity()));

	private AnthropometricFieldCatalog() {
	}

	public static List<AnthropometricFieldDefinition> allDefinitions() {
		return DEFINITIONS;
	}

	public static Optional<AnthropometricFieldDefinition> findByKey(final String fieldKey) {
		return Optional.ofNullable(BY_KEY.get(fieldKey));
	}

	public static boolean isValidKey(final String fieldKey) {
		return fieldKey != null && BY_KEY.containsKey(fieldKey);
	}

	public static Double readValue(final AnthropometricMeasurement measurement, final String fieldKey) {
		return AnthropometricFieldAccessor.read(measurement, fieldKey);
	}

	private static AnthropometricFieldDefinition field(final String key, final String label, final String unit,
			final Double min, final Double max, final Set<AnthropometricRecalcGroup> groups,
			final boolean confirmDerivedRecalc) {
		return new AnthropometricFieldDefinition(key, label, unit, min, max, groups, confirmDerivedRecalc);
	}

	private static AnthropometricFieldDefinition skinfold(final String key, final String label, final Double min,
			final Double max) {
		return field(key, label, "mm", min, max,
				Set.of(AnthropometricRecalcGroup.COMPOSITION, AnthropometricRecalcGroup.SOMATOTYPE), true);
	}

	private static AnthropometricFieldDefinition circumference(final String key, final String label, final Double min,
			final Double max) {
		return field(key, label, "cm", min, max, Set.of(AnthropometricRecalcGroup.SOMATOTYPE), true);
	}

}
