package com.nutriconsultas.clinical.exam.anthropometric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
class SomatotypeCalculationServiceTest {

	@Test
	void calculateReturnsFullSomatotypeWithReferenceInputs() {
		final SomatotypeResult result = SomatotypeCalculationService.calculate(70.0, 1.75, 10.0, 12.0, 8.0, 30.0, 36.0,
				8.0, 6.5, 9.5, 30);

		assertThat(result.isCalculable()).isTrue();
		assertThat(result.getMissingMeasurements()).isEmpty();
		assertThat(result.getEndomorphy()).isCloseTo(2.97, within(0.05));
		assertThat(result.getMesomorphy()).isCloseTo(3.98, within(0.05));
		assertThat(result.getEctomorphy()).isCloseTo(2.49, within(0.05));
		assertThat(result.getSomatocartaX()).isCloseTo(result.getEctomorphy() - result.getEndomorphy(), within(0.001));
		assertThat(result.getSomatocartaY())
			.isCloseTo(2.0 * result.getMesomorphy() - (result.getEctomorphy() + result.getEndomorphy()), within(0.001));
	}

	@Test
	void calculateEndomorphyMatchesPolynomialFormula() {
		final double endo = SomatotypeCalculationService.calculateEndomorphy(10.0, 12.0, 8.0, 175.0);
		final double sum = 30.0 * (170.18 / 175.0);
		final double expected = -0.7182 + 0.1451 * sum - 0.00068 * sum * sum + 0.0000014 * sum * sum * sum;
		assertThat(endo).isCloseTo(expected, within(0.0001));
	}

	@Test
	void calculateEctomorphyUsesHighLinearityBranch() {
		final double ecto = SomatotypeCalculationService.calculateEctomorphy(175.0, 70.0);
		final double hwr = 175.0 / Math.cbrt(70.0);
		assertThat(hwr).isGreaterThanOrEqualTo(40.75);
		assertThat(ecto).isCloseTo(0.732 * hwr - 28.58, within(0.0001));
	}

	@Test
	void calculateEctomorphyUsesMidLinearityBranch() {
		final double ecto = SomatotypeCalculationService.calculateEctomorphy(170.0, 80.0);
		final double hwr = 170.0 / Math.cbrt(80.0);
		assertThat(hwr).isBetween(38.25, 40.75);
		assertThat(ecto).isCloseTo(0.463 * hwr - 17.63, within(0.0001));
	}

	@Test
	void calculateEctomorphyUsesLowLinearityFloor() {
		final double ecto = SomatotypeCalculationService.calculateEctomorphy(165.0, 90.0);
		final double hwr = 165.0 / Math.cbrt(90.0);
		assertThat(hwr).isLessThanOrEqualTo(38.25);
		assertThat(ecto).isEqualTo(0.1);
	}

	@Test
	void clampRatingAssignsMinimumWhenNegative() {
		assertThat(SomatotypeResult.clampRating(-1.5)).isEqualTo(0.1);
		assertThat(SomatotypeResult.clampRating(0.0)).isEqualTo(0.1);
		assertThat(SomatotypeResult.clampRating(4.5)).isEqualTo(4.5);
	}

	@Test
	void calculateListsMissingMeasurements() {
		final SomatotypeResult result = SomatotypeCalculationService.calculate(70.0, 1.75, null, 12.0, 8.0, 30.0, 36.0,
				8.0, 6.5, 9.5, 30);

		assertThat(result.isCalculable()).isFalse();
		assertThat(result.getMissingMeasurements()).contains("Pliegue tríceps");
	}

	@Test
	void calculateRejectsPediatricPatients() {
		final SomatotypeResult result = SomatotypeCalculationService.calculate(50.0, 1.50, 8.0, 7.0, 6.0, 22.0, 28.0,
				6.0, 5.0, 7.5, 12);

		assertThat(result.isCalculable()).isFalse();
		assertThat(result.getMissingMeasurements())
			.contains("Paciente menor de 18 años (somatotipo solo para adultos)");
	}

}
