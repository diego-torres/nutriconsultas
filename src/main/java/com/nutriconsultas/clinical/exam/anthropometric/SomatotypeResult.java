package com.nutriconsultas.clinical.exam.anthropometric;

import java.util.Collections;
import java.util.List;

import lombok.Builder;
import lombok.Value;

/**
 * Result of a Heath-Carter somatotype calculation.
 *
 * <p>
 * Chart coordinates follow Carter &amp; Heath (1990):
 * {@code x = ectomorphy - endomorphy},
 * {@code y = 2 × mesomorphy - (ectomorphy + endomorphy)}.
 */
@Value
@Builder
public class SomatotypeResult {

	private static final double MIN_RATING = 0.1;

	Double endomorphy;

	Double mesomorphy;

	Double ectomorphy;

	Double somatocartaX;

	Double somatocartaY;

	@Builder.Default
	List<String> missingMeasurements = Collections.emptyList();

	public boolean isCalculable() {
		return endomorphy != null && mesomorphy != null && ectomorphy != null;
	}

	public static double clampRating(final double value) {
		return value <= 0 ? MIN_RATING : value;
	}

}
