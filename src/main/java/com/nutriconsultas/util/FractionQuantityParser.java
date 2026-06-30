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

		if (!trimmedGiven.contains("/")) {
			return Double.parseDouble(trimmedGiven.replace(',', '.'));
		}

		final boolean hasInteger = trimmedGiven.contains(" ");
		final int integerPart = hasInteger ? Integer.parseInt(trimmedGiven.split(" ")[0]) : 0;
		final String fractionPart = hasInteger ? trimmedGiven.split(" ")[1] : trimmedGiven;
		final String[] fractionTokens = fractionPart.split("/");
		if (fractionTokens.length != 2) {
			throw new NumberFormatException("Invalid fraction: " + given);
		}
		final int numeratorPart = Integer.parseInt(fractionTokens[0]);
		final int denominatorPart = Integer.parseInt(fractionTokens[1]);
		if (denominatorPart == 0) {
			throw new NumberFormatException("Invalid fraction denominator: " + given);
		}
		return integerPart + ((double) numeratorPart / denominatorPart);
	}

}
