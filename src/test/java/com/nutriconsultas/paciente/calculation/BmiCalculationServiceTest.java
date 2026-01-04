package com.nutriconsultas.paciente.calculation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for BMI calculation service.
 */
@DisplayName("BMI Calculation Service Tests")
class BmiCalculationServiceTest {

	@Test
	@DisplayName("Calculate Classic BMI with valid inputs")
	void testCalculateClassicBmiValid() {
		// Test case: weight = 70 kg, height = 1.75 m
		// Expected: 70 / (1.75 * 1.75) = 70 / 3.0625 = 22.86
		final Double result = BmiCalculationService.calculateClassicBmi(70.0, 1.75);
		assertThat(result).isNotNull();
		assertThat(result).isCloseTo(22.86, org.assertj.core.data.Offset.offset(0.01));
	}

	@Test
	@DisplayName("Calculate Classic BMI returns null for null weight")
	void testCalculateClassicBmiNullWeight() {
		final Double result = BmiCalculationService.calculateClassicBmi(null, 1.75);
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("Calculate Classic BMI returns null for null height")
	void testCalculateClassicBmiNullHeight() {
		final Double result = BmiCalculationService.calculateClassicBmi(70.0, null);
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("Calculate Classic BMI returns null for zero weight")
	void testCalculateClassicBmiZeroWeight() {
		final Double result = BmiCalculationService.calculateClassicBmi(0.0, 1.75);
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("Calculate Classic BMI returns null for zero height")
	void testCalculateClassicBmiZeroHeight() {
		final Double result = BmiCalculationService.calculateClassicBmi(70.0, 0.0);
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("Calculate Trefethen BMI with valid inputs")
	void testCalculateTrefethenBmiValid() {
		// Test case: weight = 70 kg, height = 1.75 m
		// Expected: 1.3 * 70 / (1.75^2.5) = 91 / 4.05 ≈ 22.47
		final Double result = BmiCalculationService.calculateTrefethenBmi(70.0, 1.75);
		assertThat(result).isNotNull();
		assertThat(result).isCloseTo(22.47, org.assertj.core.data.Offset.offset(0.1));
	}

	@Test
	@DisplayName("Calculate Trefethen BMI returns null for null weight")
	void testCalculateTrefethenBmiNullWeight() {
		final Double result = BmiCalculationService.calculateTrefethenBmi(null, 1.75);
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("Calculate Trefethen BMI returns null for null height")
	void testCalculateTrefethenBmiNullHeight() {
		final Double result = BmiCalculationService.calculateTrefethenBmi(70.0, null);
		assertThat(result).isNull();
	}

}
