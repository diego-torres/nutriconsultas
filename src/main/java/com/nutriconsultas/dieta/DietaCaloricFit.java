package com.nutriconsultas.dieta;

/**
 * Compares a diet's total kcal against a patient's caloric requirement.
 */
public final class DietaCaloricFit {

	public enum Fit {

		MATCH, UNDER, OVER, UNKNOWN

	}

	private DietaCaloricFit() {
	}

	public static Fit classify(final double dietKcal, final Double requerimientoKcal) {
		if (requerimientoKcal == null || dietKcal <= 0) {
			return Fit.UNKNOWN;
		}
		final double tolerance = Math.max(50, requerimientoKcal * 0.05);
		final double diff = dietKcal - requerimientoKcal;
		if (Math.abs(diff) <= tolerance) {
			return Fit.MATCH;
		}
		if (diff < 0) {
			return Fit.UNDER;
		}
		return Fit.OVER;
	}

	public static String label(final Fit fit, final double dietKcal, final Double requerimientoKcal) {
		if (fit == Fit.UNKNOWN || requerimientoKcal == null) {
			return "Sin comparación";
		}
		final double diff = dietKcal - requerimientoKcal;
		return switch (fit) {
			case MATCH -> "Adecuada";
			case UNDER -> "Por debajo (" + formatKcal(Math.abs(diff)) + " kcal)";
			case OVER -> "Por encima (+" + formatKcal(diff) + " kcal)";
			case UNKNOWN -> "Sin comparación";
		};
	}

	public static String cssClass(final Fit fit) {
		return switch (fit) {
			case MATCH -> "success";
			case UNDER -> "warning";
			case OVER -> "danger";
			case UNKNOWN -> "secondary";
		};
	}

	private static String formatKcal(final double value) {
		return String.valueOf(Math.round(value));
	}

}
