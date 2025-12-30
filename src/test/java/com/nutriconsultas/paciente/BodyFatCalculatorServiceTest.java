package com.nutriconsultas.paciente;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import lombok.extern.slf4j.Slf4j;

@ExtendWith(MockitoExtension.class)
@Slf4j
@ActiveProfiles("test")
public class BodyFatCalculatorServiceTest {

	private BodyFatCalculatorService service;

	@BeforeEach
	public void setup() {
		log.info("setting up BodyFatCalculatorService");
		service = new BodyFatCalculatorService();
		log.info("finished setting up BodyFatCalculatorService");
	}

	@Test
	public void testCalculateBodyFatPercentageMale() {
		log.info("starting testCalculateBodyFatPercentageMale");
		// Arrange
		final Double bmi = 25.0;
		final Integer age = 30;
		final String gender = "M";

		// Act
		final Double result = service.calculateBodyFatPercentage(bmi, age, gender);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).isBetween(5.0, 50.0);
		log.info("finished testCalculateBodyFatPercentageMale with result: {}%", result);
	}

	@Test
	public void testCalculateBodyFatPercentageFemale() {
		log.info("starting testCalculateBodyFatPercentageFemale");
		// Arrange
		final Double bmi = 25.0;
		final Integer age = 30;
		final String gender = "F";

		// Act
		final Double result = service.calculateBodyFatPercentage(bmi, age, gender);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).isBetween(5.0, 50.0);
		// Female should have higher body fat percentage than male with same BMI and age
		log.info("finished testCalculateBodyFatPercentageFemale with result: {}%", result);
	}

	@Test
	public void testCalculateBodyFatPercentageWithNullBmi() {
		log.info("starting testCalculateBodyFatPercentageWithNullBmi");
		// Arrange
		final Integer age = 30;
		final String gender = "M";

		// Act
		final Double result = service.calculateBodyFatPercentage(null, age, gender);

		// Assert
		assertThat(result).isNull();
		log.info("finished testCalculateBodyFatPercentageWithNullBmi");
	}

	@Test
	public void testCalculateBodyFatPercentageWithNullAge() {
		log.info("starting testCalculateBodyFatPercentageWithNullAge");
		// Arrange
		final Double bmi = 25.0;
		final String gender = "M";

		// Act
		final Double result = service.calculateBodyFatPercentage(bmi, null, gender);

		// Assert
		assertThat(result).isNull();
		log.info("finished testCalculateBodyFatPercentageWithNullAge");
	}

	@Test
	public void testCalculateBodyFatPercentageWithNullGender() {
		log.info("starting testCalculateBodyFatPercentageWithNullGender");
		// Arrange
		final Double bmi = 25.0;
		final Integer age = 30;

		// Act
		final Double result = service.calculateBodyFatPercentage(bmi, age, null);

		// Assert
		assertThat(result).isNull();
		log.info("finished testCalculateBodyFatPercentageWithNullGender");
	}

	@Test
	public void testCalculateBodyFatPercentageWithInvalidBmi() {
		log.info("starting testCalculateBodyFatPercentageWithInvalidBmi");
		// Arrange
		final Double bmi = -5.0;
		final Integer age = 30;
		final String gender = "M";

		// Act
		final Double result = service.calculateBodyFatPercentage(bmi, age, gender);

		// Assert
		assertThat(result).isNull();
		log.info("finished testCalculateBodyFatPercentageWithInvalidBmi");
	}

	@Test
	public void testCalculateBodyFatPercentageWithInvalidAge() {
		log.info("starting testCalculateBodyFatPercentageWithInvalidAge");
		// Arrange
		final Double bmi = 25.0;
		final Integer age = -5;
		final String gender = "M";

		// Act
		final Double result = service.calculateBodyFatPercentage(bmi, age, gender);

		// Assert
		assertThat(result).isNull();
		log.info("finished testCalculateBodyFatPercentageWithInvalidAge");
	}

	@Test
	public void testCalculateBodyFatPercentageClamping() {
		log.info("starting testCalculateBodyFatPercentageClamping");
		// Test that results are clamped to reasonable range (5-50%)
		// Arrange - Use extreme values
		final Double bmi = 50.0; // Very high BMI
		final Integer age = 80; // Very old
		final String gender = "M";

		// Act
		final Double result = service.calculateBodyFatPercentage(bmi, age, gender);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).isLessThanOrEqualTo(50.0);
		assertThat(result).isGreaterThanOrEqualTo(5.0);
		log.info("finished testCalculateBodyFatPercentageClamping with result: {}%", result);
	}

	@Test
	public void testCalculateBodyFatFromSkinfoldsMale() {
		log.info("starting testCalculateBodyFatFromSkinfoldsMale");
		// Arrange
		final Double chestSkinfold = 10.0;
		final Double abdominalSkinfold = 15.0;
		final Double thighSkinfold = 12.0;
		final Integer age = 30;
		final String gender = "M";

		// Act
		final Double result = service.calculateBodyFatFromSkinfolds(chestSkinfold, abdominalSkinfold, thighSkinfold,
				age, gender);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).isBetween(5.0, 50.0);
		log.info("finished testCalculateBodyFatFromSkinfoldsMale with result: {}%", result);
	}

	@Test
	public void testCalculateBodyFatFromSkinfoldsFemale() {
		log.info("starting testCalculateBodyFatFromSkinfoldsFemale");
		// Arrange
		final Double chestSkinfold = 15.0;
		final Double abdominalSkinfold = 20.0;
		final Double thighSkinfold = 18.0;
		final Integer age = 30;
		final String gender = "F";

		// Act
		final Double result = service.calculateBodyFatFromSkinfolds(chestSkinfold, abdominalSkinfold, thighSkinfold,
				age, gender);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).isBetween(5.0, 50.0);
		log.info("finished testCalculateBodyFatFromSkinfoldsFemale with result: {}%", result);
	}

	@Test
	public void testCalculateBodyFatFromCircumferencesMale() {
		log.info("starting testCalculateBodyFatFromCircumferencesMale");
		// Arrange
		final Double waist = 90.0; // cm
		final Double neck = 40.0; // cm
		final Double height = 175.0; // cm
		final String gender = "M";

		// Act
		final Double result = service.calculateBodyFatFromCircumferences(waist, neck, height, gender);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).isBetween(5.0, 50.0);
		log.info("finished testCalculateBodyFatFromCircumferencesMale with result: {}%", result);
	}

	@Test
	public void testCalculateBodyFatFromCircumferencesFemale() {
		log.info("starting testCalculateBodyFatFromCircumferencesFemale");
		// Arrange
		final Double waist = 80.0; // cm
		final Double neck = 35.0; // cm
		final Double height = 165.0; // cm
		final String gender = "F";

		// Act
		final Double result = service.calculateBodyFatFromCircumferences(waist, neck, height, gender);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).isBetween(5.0, 50.0);
		log.info("finished testCalculateBodyFatFromCircumferencesFemale with result: {}%", result);
	}

}
