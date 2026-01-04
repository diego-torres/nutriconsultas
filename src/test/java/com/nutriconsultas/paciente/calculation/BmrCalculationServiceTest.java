package com.nutriconsultas.paciente.calculation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for BMR calculation service.
 */
@DisplayName("BMR Calculation Service Tests")
class BmrCalculationServiceTest {

	@Test
	@DisplayName("Calculate Mifflin-St Jeor BMR for male with valid inputs")
	void testCalculateMifflinStJeorBmrMale() {
		// Test case: weight = 70 kg, height = 1.75 m (175 cm), age = 30, male
		// Expected: 10 * 70 + 6.25 * 175 - 5 * 30 + 5 = 700 + 1093.75 - 150 + 5 = 1648.75
		final Double result = BmrCalculationService.calculateMifflinStJeorBmr(70.0, 1.75, 30, true);
		assertThat(result).isNotNull();
		assertThat(result).isCloseTo(1648.75, org.assertj.core.data.Offset.offset(0.01));
	}

	@Test
	@DisplayName("Calculate Mifflin-St Jeor BMR for female with valid inputs")
	void testCalculateMifflinStJeorBmrFemale() {
		// Test case: weight = 60 kg, height = 1.65 m (165 cm), age = 25, female
		// Expected: 10 * 60 + 6.25 * 165 - 5 * 25 - 161 = 600 + 1031.25 - 125 - 161 =
		// 1345.25
		final Double result = BmrCalculationService.calculateMifflinStJeorBmr(60.0, 1.65, 25, false);
		assertThat(result).isNotNull();
		assertThat(result).isCloseTo(1345.25, org.assertj.core.data.Offset.offset(0.01));
	}

	@Test
	@DisplayName("Calculate Mifflin-St Jeor BMR returns null for null weight")
	void testCalculateMifflinStJeorBmrNullWeight() {
		final Double result = BmrCalculationService.calculateMifflinStJeorBmr(null, 1.75, 30, true);
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("Calculate Mifflin-St Jeor BMR returns null for null age")
	void testCalculateMifflinStJeorBmrNullAge() {
		final Double result = BmrCalculationService.calculateMifflinStJeorBmr(70.0, 1.75, null, true);
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("Calculate Mifflin-St Jeor BMR returns null for null isMale")
	void testCalculateMifflinStJeorBmrNullIsMale() {
		final Double result = BmrCalculationService.calculateMifflinStJeorBmr(70.0, 1.75, 30, null);
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("Calculate Harris-Benedict BMR for male with valid inputs")
	void testCalculateHarrisBenedictBmrMale() {
		// Test case: weight = 70 kg, height = 1.75 m (175 cm), age = 30, male
		// Expected: 13.397 * 70 + 4.799 * 175 - 5.677 * 30 + 88.362
		// = 937.79 + 839.825 - 170.31 + 88.362 = 1695.67
		final Double result = BmrCalculationService.calculateHarrisBenedictBmr(70.0, 1.75, 30, true);
		assertThat(result).isNotNull();
		assertThat(result).isCloseTo(1695.67, org.assertj.core.data.Offset.offset(0.1));
	}

	@Test
	@DisplayName("Calculate Harris-Benedict BMR for female with valid inputs")
	void testCalculateHarrisBenedictBmrFemale() {
		// Test case: weight = 60 kg, height = 1.65 m (165 cm), age = 25, female
		// Expected: 9.247 * 60 + 3.098 * 165 - 4.330 * 25 + 447.593
		// = 554.82 + 511.17 - 108.25 + 447.593 = 1405.33
		final Double result = BmrCalculationService.calculateHarrisBenedictBmr(60.0, 1.65, 25, false);
		assertThat(result).isNotNull();
		assertThat(result).isCloseTo(1405.33, org.assertj.core.data.Offset.offset(0.1));
	}

	@Test
	@DisplayName("Calculate Harris-Benedict BMR returns null for null weight")
	void testCalculateHarrisBenedictBmrNullWeight() {
		final Double result = BmrCalculationService.calculateHarrisBenedictBmr(null, 1.75, 30, true);
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("Calculate Katch-McArdle BMR with valid input")
	void testCalculateKatchMcArdleBmrValid() {
		// Test case: lean body mass = 55 kg
		// Expected: 370 + (21.6 * 55) = 370 + 1188 = 1558
		final Double result = BmrCalculationService.calculateKatchMcArdleBmr(55.0);
		assertThat(result).isNotNull();
		assertThat(result).isCloseTo(1558.0, org.assertj.core.data.Offset.offset(0.01));
	}

	@Test
	@DisplayName("Calculate Katch-McArdle BMR returns null for null input")
	void testCalculateKatchMcArdleBmrNull() {
		final Double result = BmrCalculationService.calculateKatchMcArdleBmr(null);
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("Calculate Katch-McArdle BMR returns null for zero input")
	void testCalculateKatchMcArdleBmrZero() {
		final Double result = BmrCalculationService.calculateKatchMcArdleBmr(0.0);
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("Calculate Cunningham BMR with valid input")
	void testCalculateCunninghamBmrValid() {
		// Test case: lean body mass = 55 kg
		// Expected: 500 + (22 * 55) = 500 + 1210 = 1710
		final Double result = BmrCalculationService.calculateCunninghamBmr(55.0);
		assertThat(result).isNotNull();
		assertThat(result).isCloseTo(1710.0, org.assertj.core.data.Offset.offset(0.01));
	}

	@Test
	@DisplayName("Calculate Cunningham BMR returns null for null input")
	void testCalculateCunninghamBmrNull() {
		final Double result = BmrCalculationService.calculateCunninghamBmr(null);
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("Calculate Cunningham BMR returns null for zero input")
	void testCalculateCunninghamBmrZero() {
		final Double result = BmrCalculationService.calculateCunninghamBmr(0.0);
		assertThat(result).isNull();
	}

}
