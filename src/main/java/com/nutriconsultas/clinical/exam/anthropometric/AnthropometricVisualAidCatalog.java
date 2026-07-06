package com.nutriconsultas.clinical.exam.anthropometric;

import java.util.Map;
import java.util.Optional;

/**
 * Static images that illustrate how to take anthropometric measurements (pliegues, etc.).
 */
public final class AnthropometricVisualAidCatalog {

	private static final String PLIEGUES_BASE_PATH = "/sbadmin/img/anthropometry/pliegues/";

	private static final Map<String, String> PLIEGUES_IMAGES = Map.of(
			"skinfolds.subscapularSkinfold", "subescapular.png",
			"skinfolds.tricepsSkinfold", "triceps.png",
			"skinfolds.bicepsSkinfold", "biceps.png",
			"skinfolds.iliacCrestSkinfold", "cresta-iliaca.png",
			"skinfolds.supraespinalSkinfold", "supraespinal.png",
			"skinfolds.abdominalSkinfold", "abdominal.png",
			"skinfolds.frontalThighSkinfold", "muslo-frontal.png",
			"skinfolds.medialCalfSkinfold", "pantorrilla-medial.png",
			"skinfolds.medialAxillarySkinfold", "axilar-medial.png",
			"skinfolds.pectoralSkinfold", "pectoral.png");

	private AnthropometricVisualAidCatalog() {
	}

	public static boolean isSkinfoldField(final String fieldKey) {
		return fieldKey != null && fieldKey.startsWith("skinfolds.");
	}

	public static Optional<String> imageFileName(final String fieldKey) {
		if (fieldKey == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(PLIEGUES_IMAGES.get(fieldKey));
	}

	public static Optional<String> imagePath(final String fieldKey) {
		return imageFileName(fieldKey).map(fileName -> PLIEGUES_BASE_PATH + fileName);
	}

}
