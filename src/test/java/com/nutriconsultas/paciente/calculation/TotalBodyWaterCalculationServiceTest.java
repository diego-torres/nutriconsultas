package com.nutriconsultas.paciente.calculation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for total body water calculation service.
 */
@DisplayName("Total Body Water Calculation Service Tests")
class TotalBodyWaterCalculationServiceTest {

	@Test
	@DisplayName("Calculate Watson TBW for male with valid inputs")
	void testCalculateWatsonTbwMale() {
		// Test case: weight = 70 kg, height = 1.70 m (170 cm), age = 30, male
		// Expected: 2.447 - (0.09156 * 30) + (0.1074 * 170) + (0.3362 * 70)
		// = 2.447 - 2.7468 + 18.258 + 23.534 = 41.4922
		final Double result = TotalBodyWaterCalculationService.calculateWatsonTbw(70.0, 1.70, 30, true);
		assertThat(result).isNotNull();
		assertThat(result).isCloseTo(41.49, org.assertj.core.data.Offset.offset(0.1));
	}

	@Test
	@DisplayName("Calculate Watson TBW for female with valid inputs")
	void testCalculateWatsonTbwFemale() {
		// Test case: weight = 60 kg, height = 1.65 m (165 cm), female
		// Expected: -2.097 + (0.1069 * 165) + (0.2466 * 60)
		// = -2.097 + 17.6385 + 14.796 = 30.3375
		final Double result = TotalBodyWaterCalculationService.calculateWatsonTbw(60.0, 1.65, 25, false);
		assertThat(result).isNotNull();
		assertThat(result).isCloseTo(30.34, org.assertj.core.data.Offset.offset(0.1));
	}

	@Test
	@DisplayName("Calculate Watson TBW returns null for null weight")
	void testCalculateWatsonTbwNullWeight() {
		final Double result = TotalBodyWaterCalculationService.calculateWatsonTbw(null, 1.70, 30, true);
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("Calculate Watson TBW returns null for null height")
	void testCalculateWatsonTbwNullHeight() {
		final Double result = TotalBodyWaterCalculationService.calculateWatsonTbw(70.0, null, 30, true);
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("Calculate Watson TBW returns null for null isMale")
	void testCalculateWatsonTbwNullIsMale() {
		final Double result = TotalBodyWaterCalculationService.calculateWatsonTbw(70.0, 1.70, 30, null);
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("Calculate Watson TBW returns null for male with null age")
	void testCalculateWatsonTbwMaleNullAge() {
		final Double result = TotalBodyWaterCalculationService.calculateWatsonTbw(70.0, 1.70, null, true);
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("Calculate Watson TBW returns null for male with zero age")
	void testCalculateWatsonTbwMaleZeroAge() {
		final Double result = TotalBodyWaterCalculationService.calculateWatsonTbw(70.0, 1.70, 0, true);
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("Calculate Watson TBW for female with null age (age not required)")
	void testCalculateWatsonTbwFemaleNullAge() {
		// Age is not required for women in Watson formula
		final Double result = TotalBodyWaterCalculationService.calculateWatsonTbw(60.0, 1.65, null, false);
		assertThat(result).isNotNull();
		assertThat(result).isCloseTo(30.34, org.assertj.core.data.Offset.offset(0.1));
	}

	@Test
	@DisplayName("Calculate Hume-Meyers TBW for male with valid inputs")
	void testCalculateHumeMeyersTbwMale() {
		// Test case: weight = 70 kg, height = 1.70 m (170 cm), male
		// Expected: (0.194786 * 170) + (0.296785 * 70) - 14.012934
		// = 33.11362 + 20.77495 - 14.012934 = 39.875636
		final Double result = TotalBodyWaterCalculationService.calculateHumeMeyersTbw(70.0, 1.70, true);
		assertThat(result).isNotNull();
		assertThat(result).isCloseTo(39.88, org.assertj.core.data.Offset.offset(0.1));
	}

	@Test
	@DisplayName("Calculate Hume-Meyers TBW for female with valid inputs")
	void testCalculateHumeMeyersTbwFemale() {
		// Test case: weight = 60 kg, height = 1.65 m (165 cm), female
		// Expected: (0.344547 * 165) + (0.183809 * 60) - 35.270121
		// = 56.850255 + 11.02854 - 35.270121 = 32.608674
		final Double result = TotalBodyWaterCalculationService.calculateHumeMeyersTbw(60.0, 1.65, false);
		assertThat(result).isNotNull();
		assertThat(result).isCloseTo(32.61, org.assertj.core.data.Offset.offset(0.1));
	}

	@Test
	@DisplayName("Calculate Hume-Meyers TBW returns null for null weight")
	void testCalculateHumeMeyersTbwNullWeight() {
		final Double result = TotalBodyWaterCalculationService.calculateHumeMeyersTbw(null, 1.70, true);
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("Calculate Hume-Meyers TBW returns null for null height")
	void testCalculateHumeMeyersTbwNullHeight() {
		final Double result = TotalBodyWaterCalculationService.calculateHumeMeyersTbw(70.0, null, true);
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("Calculate Hume-Meyers TBW returns null for null isMale")
	void testCalculateHumeMeyersTbwNullIsMale() {
		final Double result = TotalBodyWaterCalculationService.calculateHumeMeyersTbw(70.0, 1.70, null);
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("Calculate Hume-Meyers TBW returns null for zero weight")
	void testCalculateHumeMeyersTbwZeroWeight() {
		final Double result = TotalBodyWaterCalculationService.calculateHumeMeyersTbw(0.0, 1.70, true);
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("Calculate Hume-Meyers TBW returns null for zero height")
	void testCalculateHumeMeyersTbwZeroHeight() {
		final Double result = TotalBodyWaterCalculationService.calculateHumeMeyersTbw(70.0, 0.0, true);
		assertThat(result).isNull();
	}

}
