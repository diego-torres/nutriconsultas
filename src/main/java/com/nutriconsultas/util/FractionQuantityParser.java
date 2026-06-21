package com.nutriconsultas.util;

/**
 * Parses fractional portion quantities entered in admin dialogs (e.g. {@code 1 1/2},
 * {@code 1/2}, {@code 2}).
 */
public final class FractionQuantityParser {

	private FractionQuantityParser() {
	}

	public static Double parseFractionalQuantity(final String given) {
		if (given == null) {
			return null;
		}
		final String trimmedGiven = given.trim();
		if (trimmedGiven.isEmpty()) {
			return null;
		}

		final boolean hasInteger = trimmedGiven.contains(" ") || !trimmedGiven.contains("/");
		final boolean hasFraction = trimmedGiven.contains("/");
		final int integerPart = hasInteger ? Integer.parseInt(trimmedGiven.split(" ")[0]) : 0;
		final int numeratorPart = hasInteger
				? hasFraction ? Integer.parseInt(trimmedGiven.split(" ")[1].split("/")[0]) : 0
				: Integer.parseInt(trimmedGiven.split("/")[0]);
		final int denominatorPart = hasInteger
				? hasFraction ? Integer.parseInt(trimmedGiven.split(" ")[1].split("/")[1]) : 0
				: Integer.parseInt(trimmedGiven.split("/")[1]);
		final double fractionalValue = hasFraction ? (double) numeratorPart / denominatorPart : 0d;
		return integerPart + fractionalValue;
	}

}
