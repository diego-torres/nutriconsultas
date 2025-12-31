package com.nutriconsultas.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@MappedSuperclass
@EqualsAndHashCode(callSuper = false)
public abstract class AbstractFraccionable extends AbstractNutrible {

	@Column(precision = 5)
	protected Double cantSugerida;

	public String getFractionalCantSugerida() {
		if (cantSugerida == null) {
			return "";
		}
		final Integer intPart = cantSugerida.intValue();
		// convert the fractional part to a fraction
		final Double fractionalPart = cantSugerida - intPart;
		final Double tolerance = 1.0E-6;
		Double h1 = 1d;
		Double h2 = 0d;
		Double k1 = 0d;
		Double k2 = 1d;
		Double b = fractionalPart;
		do {
			final Double a = Math.floor(b);
			final Double aux = h1;
			h1 = a * h1 + h2;
			h2 = aux;
			final Double aux2 = k1;
			k1 = a * k1 + k2;
			k2 = aux2;
			b = 1 / (b - a);
		}
		while (Math.abs(fractionalPart - h1 / k1) > fractionalPart * tolerance);

		return intPart > 0 ? intPart + " " : "" + h1.intValue() + "/" + k1.intValue();
	}

	/**
	 * Returns a rounded fractional quantity rounded to the nearest 0.25 increment. This
	 * produces clean fractions like 1/4, 1/2, 3/4, 1, 1 1/4, etc., avoiding awkward
	 * fractions like 6/23.
	 * @return formatted string with rounded fractional quantity
	 */
	public String getRoundedFractionalCantSugerida() {
		if (cantSugerida == null) {
			return "";
		}
		// Round to nearest 0.25 (1/4) increment
		final Double rounded = Math.round(cantSugerida * 4.0) / 4.0;
		final Integer intPart = rounded.intValue();
		final Double fractionalPart = rounded - intPart;

		// Convert fractional part to clean fraction (only 0, 1/4, 1/2, or 3/4)
		if (Math.abs(fractionalPart) < 0.01) {
			// No fraction, just the integer part
			return intPart > 0 ? String.valueOf(intPart) : "";
		}
		else if (Math.abs(fractionalPart - 0.25) < 0.01) {
			// 1/4
			return intPart > 0 ? intPart + " 1/4" : "1/4";
		}
		else if (Math.abs(fractionalPart - 0.5) < 0.01) {
			// 1/2
			return intPart > 0 ? intPart + " 1/2" : "1/2";
		}
		else if (Math.abs(fractionalPart - 0.75) < 0.01) {
			// 3/4
			return intPart > 0 ? intPart + " 3/4" : "3/4";
		}
		else {
			// Fallback (shouldn't happen with rounding, but just in case)
			return intPart > 0 ? String.valueOf(intPart) : "";
		}
	}

}
