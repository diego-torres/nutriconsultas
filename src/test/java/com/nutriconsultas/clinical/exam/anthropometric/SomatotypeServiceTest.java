package com.nutriconsultas.clinical.exam.anthropometric;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.nutriconsultas.clinical.exam.AnthropometricMeasurement;
import com.nutriconsultas.paciente.Paciente;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class SomatotypeServiceTest {

	@InjectMocks
	private SomatotypeService service;

	private Paciente paciente;

	@BeforeEach
	void setUp() {
		paciente = new Paciente();
		paciente.setId(1L);
		final LocalDate dob = LocalDate.now().minusYears(30);
		paciente.setDob(Date.from(dob.atStartOfDay(ZoneId.systemDefault()).toInstant()));
	}

	@Test
	void applyToMeasurementPersistsSomatotypeWhenInputsComplete() {
		final AnthropometricMeasurement measurement = createCompleteMeasurement();

		final SomatotypeResult result = service.applyToMeasurement(measurement, paciente);

		assertThat(result.isCalculable()).isTrue();
		assertThat(measurement.getEndomorphy()).isEqualTo(result.getEndomorphy());
		assertThat(measurement.getMesomorphy()).isEqualTo(result.getMesomorphy());
		assertThat(measurement.getEctomorphy()).isEqualTo(result.getEctomorphy());
		assertThat(measurement.getSomatocartaX()).isEqualTo(result.getSomatocartaX());
		assertThat(measurement.getSomatocartaY()).isEqualTo(result.getSomatocartaY());
	}

	@Test
	void applyToMeasurementClearsSomatotypeWhenInputsIncomplete() {
		final AnthropometricMeasurement measurement = createCompleteMeasurement();
		service.applyToMeasurement(measurement, paciente);
		measurement.getSkinfolds().setTricepsSkinfold(null);

		final SomatotypeResult result = service.applyToMeasurement(measurement, paciente);

		assertThat(result.isCalculable()).isFalse();
		assertThat(measurement.getEndomorphy()).isNull();
		assertThat(measurement.getMesomorphy()).isNull();
		assertThat(measurement.getEctomorphy()).isNull();
	}

	private AnthropometricMeasurement createCompleteMeasurement() {
		final AnthropometricMeasurement measurement = new AnthropometricMeasurement();
		final BodyMass bodyMass = new BodyMass();
		bodyMass.setWeight(70.0);
		bodyMass.setHeight(1.75);
		measurement.setBodyMass(bodyMass);

		final Skinfolds skinfolds = new Skinfolds();
		skinfolds.setTricepsSkinfold(10.0);
		skinfolds.setSubscapularSkinfold(12.0);
		skinfolds.setSupraespinalSkinfold(8.0);
		skinfolds.setMedialCalfSkinfold(8.0);
		measurement.setSkinfolds(skinfolds);

		final Circumferences circumferences = new Circumferences();
		circumferences.setMidUpperArmCircumferenceContracted(30.0);
		circumferences.setCalfCircumference(36.0);
		measurement.setCircumferences(circumferences);

		final Diameters diameters = new Diameters();
		diameters.setHumerusDiameter(6.5);
		diameters.setFemurDiameter(9.5);
		measurement.setDiameters(diameters);

		return measurement;
	}

}
