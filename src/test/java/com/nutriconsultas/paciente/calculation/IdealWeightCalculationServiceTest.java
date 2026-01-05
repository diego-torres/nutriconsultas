package com.nutriconsultas.paciente.calculation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ideal weight calculation service.
 */
@DisplayName("Ideal Weight Calculation Service Tests")
class IdealWeightCalculationServiceTest {

	@Test
	@DisplayName("Calculate Robinson ideal weight for male with valid input")
	void testCalculateRobinsonIdealWeightMale() {
		// Test case: height = 1.75 m (68.9 inches), male
		// Expected: 52 + 1.9 * (68.9 - 60) = 52 + 1.9 * 8.9 = 52 + 16.91 = 68.91
		final Double result = IdealWeightCalculationService.calculateRobinsonIdealWeight(1.75, true);
		assertThat(result).isNotNull();
		assertThat(result).isCloseTo(68.91, org.assertj.core.data.Offset.offset(0.1));
	}

	@Test
	@DisplayName("Calculate Robinson ideal weight for female with valid input")
	void testCalculateRobinsonIdealWeightFemale() {
		// Test case: height = 1.65 m (64.96 inches), female
		// Expected: 49 + 1.7 * (64.96 - 60) = 49 + 1.7 * 4.96 = 49 + 8.432 = 57.432
		final Double result = IdealWeightCalculationService.calculateRobinsonIdealWeight(1.65, false);
		assertThat(result).isNotNull();
		assertThat(result).isCloseTo(57.43, org.assertj.core.data.Offset.offset(0.1));
	}

	@Test
	@DisplayName("Calculate Robinson ideal weight returns null for null height")
	void testCalculateRobinsonIdealWeightNullHeight() {
		final Double result = IdealWeightCalculationService.calculateRobinsonIdealWeight(null, true);
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("Calculate Robinson ideal weight returns null for null isMale")
	void testCalculateRobinsonIdealWeightNullIsMale() {
		final Double result = IdealWeightCalculationService.calculateRobinsonIdealWeight(1.75, null);
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("Calculate Metropolitan ideal weight for male with valid input")
	void testCalculateMetropolitanIdealWeightMale() {
		// Test case: height = 1.75 m (68.9 inches), male
		// Expected: 50.0 + 2.3 * (68.9 - 60) = 50.0 + 2.3 * 8.9 = 50.0 + 20.47 = 70.47
		final Double result = IdealWeightCalculationService.calculateMetropolitanIdealWeight(1.75, true);
		assertThat(result).isNotNull();
		assertThat(result).isCloseTo(70.47, org.assertj.core.data.Offset.offset(0.1));
	}

	@Test
	@DisplayName("Calculate Metropolitan ideal weight for female with valid input")
	void testCalculateMetropolitanIdealWeightFemale() {
		// Test case: height = 1.65 m (64.96 inches), female
		// Expected: 45.5 + 2.3 * (64.96 - 60) = 45.5 + 2.3 * 4.96 = 45.5 + 11.408 =
		// 56.908
		final Double result = IdealWeightCalculationService.calculateMetropolitanIdealWeight(1.65, false);
		assertThat(result).isNotNull();
		assertThat(result).isCloseTo(56.91, org.assertj.core.data.Offset.offset(0.1));
	}

	@Test
	@DisplayName("Calculate Metropolitan ideal weight returns null for null height")
	void testCalculateMetropolitanIdealWeightNullHeight() {
		final Double result = IdealWeightCalculationService.calculateMetropolitanIdealWeight(null, true);
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("Calculate Hamwi ideal weight for male with valid input")
	void testCalculateHamwiIdealWeightMale() {
		// Test case: height = 1.75 m (68.9 inches), male
		// Expected: 48.0 + 2.7 * (68.9 - 60) = 48.0 + 2.7 * 8.9 = 48.0 + 24.03 = 72.03
		final Double result = IdealWeightCalculationService.calculateHamwiIdealWeight(1.75, true);
		assertThat(result).isNotNull();
		assertThat(result).isCloseTo(72.03, org.assertj.core.data.Offset.offset(0.1));
	}

	@Test
	@DisplayName("Calculate Hamwi ideal weight for female with valid input")
	void testCalculateHamwiIdealWeightFemale() {
		// Test case: height = 1.65 m (64.96 inches), female
		// Expected: 45.5 + 2.2 * (64.96 - 60) = 45.5 + 2.2 * 4.96 = 45.5 + 10.912 =
		// 56.412
		final Double result = IdealWeightCalculationService.calculateHamwiIdealWeight(1.65, false);
		assertThat(result).isNotNull();
		assertThat(result).isCloseTo(56.41, org.assertj.core.data.Offset.offset(0.1));
	}

	@Test
	@DisplayName("Calculate Hamwi ideal weight returns null for null height")
	void testCalculateHamwiIdealWeightNullHeight() {
		final Double result = IdealWeightCalculationService.calculateHamwiIdealWeight(null, true);
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("Calculate Lorentz ideal weight for male with valid input")
	void testCalculateLorentzIdealWeightMale() {
		// Test case: height = 1.75 m (175 cm), male
		// Expected: (175 - 100) - ((175 - 150) / 4) = 75 - (25 / 4) = 75 - 6.25 = 68.75
		final Double result = IdealWeightCalculationService.calculateLorentzIdealWeight(1.75, true);
		assertThat(result).isNotNull();
		assertThat(result).isCloseTo(68.75, org.assertj.core.data.Offset.offset(0.01));
	}

	@Test
	@DisplayName("Calculate Lorentz ideal weight for female with valid input")
	void testCalculateLorentzIdealWeightFemale() {
		// Test case: height = 1.65 m (165 cm), female
		// Expected: (165 - 100) - ((165 - 150) / 2) = 65 - (15 / 2) = 65 - 7.5 = 57.5
		final Double result = IdealWeightCalculationService.calculateLorentzIdealWeight(1.65, false);
		assertThat(result).isNotNull();
		assertThat(result).isCloseTo(57.5, org.assertj.core.data.Offset.offset(0.01));
	}

	@Test
	@DisplayName("Calculate Lorentz ideal weight returns null for null height")
	void testCalculateLorentzIdealWeightNullHeight() {
		final Double result = IdealWeightCalculationService.calculateLorentzIdealWeight(null, true);
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("Calculate Traditional ideal weight with valid input")
	void testCalculateTraditionalIdealWeightValid() {
		// Test case: height = 1.75 m (175 cm)
		// Expected: 175 - 100 = 75
		final Double result = IdealWeightCalculationService.calculateTraditionalIdealWeight(1.75);
		assertThat(result).isNotNull();
		assertThat(result).isCloseTo(75.0, org.assertj.core.data.Offset.offset(0.01));
	}

	@Test
	@DisplayName("Calculate Traditional ideal weight returns null for null height")
	void testCalculateTraditionalIdealWeightNull() {
		final Double result = IdealWeightCalculationService.calculateTraditionalIdealWeight(null);
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("Calculate Traditional ideal weight returns null for zero height")
	void testCalculateTraditionalIdealWeightZero() {
		final Double result = IdealWeightCalculationService.calculateTraditionalIdealWeight(0.0);
		assertThat(result).isNull();
	}

}
