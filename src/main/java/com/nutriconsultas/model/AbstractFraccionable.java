package com.nutriconsultas.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@MappedSuperclass
@EqualsAndHashCode(callSuper = false)
public abstract class AbstractFraccionable extends AbstractNutrible {

	private static final double MATCH_EPSILON = 1.0E-9;

	private static final StandardFraction[] STANDARD_FRACTIONS = { new StandardFraction(0.0, "", 0),
			new StandardFraction(0.25, "1/4", 4), new StandardFraction(1.0 / 3.0, "1/3", 3),
			new StandardFraction(0.5, "1/2", 2), new StandardFraction(2.0 / 3.0, "2/3", 3),
			new StandardFraction(0.75, "3/4", 4) };

	@Column(precision = 5)
	protected Double cantSugerida;

	/**
	 * Returns a human-friendly fractional quantity rounded to the nearest standard
	 * cooking fraction among wholes, halves, quarters, and thirds.
	 */
	public String getFractionalCantSugerida() {
		return getRoundedFractionalCantSugerida();
	}

	/**
	 * Returns a rounded fractional quantity using the nearest value among {@code n},
	 * {@code n + 1/4}, {@code n + 1/3}, {@code n + 1/2}, {@code n + 2/3}, and
	 * {@code n + 3/4}.
	 * @return formatted string with rounded fractional quantity
	 */
	public String getRoundedFractionalCantSugerida() {
		if (cantSugerida == null) {
			return "";
		}
		final int baseInt = (int) Math.floor(cantSugerida);
		double bestDiff = Double.MAX_VALUE;
		int bestIntPart = 0;
		StandardFraction bestFraction = STANDARD_FRACTIONS[0];

		for (int intPart = baseInt; intPart <= baseInt + 1; intPart++) {
			for (final StandardFraction fraction : STANDARD_FRACTIONS) {
				final double candidate = intPart + fraction.value();
				final double diff = Math.abs(cantSugerida - candidate);
				if (isBetterFractionMatch(diff, bestDiff, fraction, bestFraction)) {
					bestDiff = diff;
					bestIntPart = intPart;
					bestFraction = fraction;
				}
			}
		}

		return formatRoundedQuantity(bestIntPart, bestFraction.label());
	}

	/**
	 * Whether ingredient quantity should be shown as rounded grams
	 * ({@code pesoBrutoRedondeado}) instead of a cooking fraction. Applies to gram units
	 * and very small taza portions.
	 */
	public boolean shouldDisplayWeightInGrams(final String unidad) {
		if (unidad == null || getPesoBrutoRedondeado() == null) {
			return false;
		}
		if ("g".equals(unidad)) {
			return true;
		}
		return "taza".equals(unidad) && cantSugerida != null && cantSugerida < 0.25;
	}

	/**
	 * Quantity string for UI and PDF: grams when
	 * {@link #shouldDisplayWeightInGrams(String)} applies, otherwise the nearest standard
	 * cooking fraction.
	 */
	public String getDisplayCantSugerida(final String unidad) {
		if (shouldDisplayWeightInGrams(unidad)) {
			return String.valueOf(getPesoBrutoRedondeado());
		}
		return getRoundedFractionalCantSugerida();
	}

	private static boolean isBetterFractionMatch(final double diff, final double bestDiff,
			final StandardFraction candidate, final StandardFraction currentBest) {
		if (diff + MATCH_EPSILON < bestDiff) {
			return true;
		}
		if (Math.abs(diff - bestDiff) > MATCH_EPSILON) {
			return false;
		}
		if (candidate.denominatorRank() != currentBest.denominatorRank()) {
			return candidate.denominatorRank() < currentBest.denominatorRank();
		}
		return candidate.value() < currentBest.value();
	}

	private static String formatRoundedQuantity(final int intPart, final String fractionLabel) {
		if (fractionLabel.isEmpty()) {
			return intPart > 0 ? String.valueOf(intPart) : "";
		}
		if (intPart > 0) {
			return intPart + " " + fractionLabel;
		}
		return fractionLabel;
	}

	/**
	 * Net weight (g) per one reference portion unit ({@code pesoNeto / cantSugerida}).
	 */
	public Double getPesoUnitario() {
		if (cantSugerida == null || cantSugerida == 0 || getPesoNeto() == null) {
			return null;
		}
		return getPesoNeto().doubleValue() / cantSugerida;
	}

	private record StandardFraction(double value, String label, int denominatorRank) {
	}

}
