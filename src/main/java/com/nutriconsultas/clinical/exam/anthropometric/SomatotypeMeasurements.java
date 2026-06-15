package com.nutriconsultas.clinical.exam.anthropometric;

import lombok.Builder;
import lombok.Value;

/** Raw anthropometric inputs for Heath-Carter somatotype calculation. */
@Value
@Builder(toBuilder = true)
public class SomatotypeMeasurements {

	Double weightKg;

	Double heightMeters;

	Double tricepsSkinfoldMm;

	Double subscapularSkinfoldMm;

	Double supraspinalSkinfoldMm;

	Double flexedArmGirthCm;

	Double calfGirthCm;

	Double medialCalfSkinfoldMm;

	Double humerusBreadthCm;

	Double femurBreadthCm;

}
