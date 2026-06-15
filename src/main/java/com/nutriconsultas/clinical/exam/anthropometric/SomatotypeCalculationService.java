package com.nutriconsultas.clinical.exam.anthropometric;

import java.util.ArrayList;
import java.util.List;

/**
 * Heath-Carter anthropometric somatotype (classic method, Carter &amp; Heath 1990).
 *
 * <p>
 * Formulas (metric units):
 * <ul>
 * <li><b>Endomorphy:</b> {@code -0.7182 + 0.1451X - 0.00068X² + 0.0000014X³} where
 * {@code X = (triceps + subscapular + supraspinal) × (170.18 / heightCm)}</li>
 * <li><b>Mesomorphy:</b>
 * {@code 0.858×humerus + 0.601×femur + 0.188×correctedArm + 0.161×correctedCalf - 0.131×heightCm + 4.5}
 * with corrected girths = girth - skinfold/10</li>
 * <li><b>Ectomorphy:</b> from HWR = heightCm / ∛weightKg; piecewise linear (see
 * {@link #calculateEctomorphy(double, double)})</li>
 * </ul>
 * Ratings ≤ 0 are assigned 0.1 per Heath-Carter convention. Same formulas apply to both
 * sexes; reference norms differ by sex but calculation does not.
 */
public final class SomatotypeCalculationService {

	public static final int MIN_ADULT_AGE_YEARS = 18;

	private static final double HEIGHT_CORRECTION = 170.18;

	private static final double ENDO_CUBIC_COEFFICIENT = 0.000_001_4;

	private static final double ENDO_QUADRATIC_COEFFICIENT = 0.000_68;

	private SomatotypeCalculationService() {
	}

	public static SomatotypeResult calculate(final SomatotypeMeasurements measurements, final Integer patientAgeYears) {
		if (patientAgeYears != null && patientAgeYears < MIN_ADULT_AGE_YEARS) {
			return SomatotypeResult.builder()
				.missingMeasurements(List.of("Paciente menor de 18 años (somatotipo solo para adultos)"))
				.build();
		}

		final List<String> missing = collectMissingMeasurements(measurements);
		if (!missing.isEmpty()) {
			return SomatotypeResult.builder().missingMeasurements(missing).build();
		}

		final double heightCm = measurements.getHeightMeters() * 100.0;
		final double endomorphy = SomatotypeResult.clampRating(calculateEndomorphy(measurements.getTricepsSkinfoldMm(),
				measurements.getSubscapularSkinfoldMm(), measurements.getSupraspinalSkinfoldMm(), heightCm));
		final double correctedArm = measurements.getFlexedArmGirthCm() - measurements.getTricepsSkinfoldMm() / 10.0;
		final double correctedCalf = measurements.getCalfGirthCm() - measurements.getMedialCalfSkinfoldMm() / 10.0;
		final double mesomorphy = SomatotypeResult.clampRating(calculateMesomorphy(correctedArm, correctedCalf,
				measurements.getHumerusBreadthCm(), measurements.getFemurBreadthCm(), heightCm));
		final double ectomorphy = SomatotypeResult
			.clampRating(calculateEctomorphy(heightCm, measurements.getWeightKg()));
		final double chartX = ectomorphy - endomorphy;
		final double chartY = 2.0 * mesomorphy - (ectomorphy + endomorphy);

		return SomatotypeResult.builder()
			.endomorphy(endomorphy)
			.mesomorphy(mesomorphy)
			.ectomorphy(ectomorphy)
			.somatocartaX(chartX)
			.somatocartaY(chartY)
			.missingMeasurements(List.of())
			.build();
	}

	static double calculateEndomorphy(final double tricepsMm, final double subscapularMm, final double supraspinalMm,
			final double heightCm) {
		final double sumSkinfolds = tricepsMm + subscapularMm + supraspinalMm;
		final double x = sumSkinfolds * (HEIGHT_CORRECTION / heightCm);
		return -0.7182 + 0.1451 * x - ENDO_QUADRATIC_COEFFICIENT * x * x + ENDO_CUBIC_COEFFICIENT * x * x * x;
	}

	static double calculateMesomorphy(final double correctedArmGirthCm, final double correctedCalfGirthCm,
			final double humerusBreadthCm, final double femurBreadthCm, final double heightCm) {
		return 0.858 * humerusBreadthCm + 0.601 * femurBreadthCm + 0.188 * correctedArmGirthCm
				+ 0.161 * correctedCalfGirthCm - 0.131 * heightCm + 4.5;
	}

	static double calculateEctomorphy(final double heightCm, final double weightKg) {
		final double hwr = heightCm / Math.cbrt(weightKg);
		if (hwr >= 40.75) {
			return 0.732 * hwr - 28.58;
		}
		if (hwr > 38.25) {
			return 0.463 * hwr - 17.63;
		}
		return 0.1;
	}

	private static List<String> collectMissingMeasurements(final SomatotypeMeasurements measurements) {
		final List<String> missing = new ArrayList<>();
		if (measurements.getWeightKg() == null || measurements.getWeightKg() <= 0) {
			missing.add("Peso");
		}
		if (measurements.getHeightMeters() == null || measurements.getHeightMeters() <= 0) {
			missing.add("Estatura");
		}
		if (measurements.getTricepsSkinfoldMm() == null) {
			missing.add("Pliegue tríceps");
		}
		if (measurements.getSubscapularSkinfoldMm() == null) {
			missing.add("Pliegue subescapular");
		}
		if (measurements.getSupraspinalSkinfoldMm() == null) {
			missing.add("Pliegue supraespinal");
		}
		if (measurements.getFlexedArmGirthCm() == null) {
			missing.add("Perímetro brazo contraído");
		}
		if (measurements.getCalfGirthCm() == null) {
			missing.add("Perímetro pantorrilla");
		}
		if (measurements.getMedialCalfSkinfoldMm() == null) {
			missing.add("Pliegue pantorrilla medial");
		}
		if (measurements.getHumerusBreadthCm() == null) {
			missing.add("Diámetro húmero");
		}
		if (measurements.getFemurBreadthCm() == null) {
			missing.add("Diámetro fémur");
		}
		return missing;
	}

}
