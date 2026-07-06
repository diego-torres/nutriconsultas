package com.nutriconsultas.util;

import com.nutriconsultas.clinical.exam.anthropometric.AnthropometricVisualAidCatalog;

/**
 * Thymeleaf helper ({@code #anthropometricVisualAid}) for anthropometric measurement
 * visual aids.
 */
public final class AnthropometricVisualAidUtils {

	private AnthropometricVisualAidUtils() {
	}

	public static boolean isSkinfoldField(final String fieldKey) {
		return AnthropometricVisualAidCatalog.isSkinfoldField(fieldKey);
	}

	public static String imageFileName(final String fieldKey) {
		return AnthropometricVisualAidCatalog.imageFileName(fieldKey).orElse(null);
	}

	public static boolean hasVisualAid(final String fieldKey) {
		return AnthropometricVisualAidCatalog.imageFileName(fieldKey).isPresent();
	}

}
