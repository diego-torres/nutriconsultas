package com.nutriconsultas.clinical.exam.anthropometric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.nutriconsultas.clinical.exam.AnthropometricMeasurement;
import com.nutriconsultas.paciente.BodyFatCalculatorService;
import com.nutriconsultas.paciente.Paciente;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class BodyCompositionServiceTest {

	@InjectMocks
	private BodyCompositionService service;

	@Mock
	private BodyFatCalculatorService bodyFatCalculatorService;

	private Paciente paciente;

	@BeforeEach
	void setUp() {
		paciente = new Paciente();
		paciente.setId(1L);
		paciente.setGender("M");
		final LocalDate dob = LocalDate.now().minusYears(30);
		paciente.setDob(Date.from(dob.atStartOfDay(ZoneId.systemDefault()).toInstant()));
	}

	@Test
	void applyToMeasurementUsesManualPorcentajeGrasaCorporal() {
		final AnthropometricMeasurement measurement = createMeasurementWithWeight(70.0, 1.75);
		measurement.setPorcentajeGrasaCorporal(18.5);

		service.applyToMeasurement(measurement, paciente, 22.86);

		assertThat(measurement.getPorcentajeGrasaCorporal()).isEqualTo(18.5);
		assertThat(measurement.getIndiceGrasaCorporal()).isEqualTo(18.5);
		assertThat(measurement.getPorcentajeMasaMuscular()).isEqualTo(80.0);
		assertThat(measurement.getMetodoObtencion()).isEqualTo(MetodoObtencionComposicionCorporal.MANUAL);
		verify(bodyFatCalculatorService, never()).calculateBodyFatPercentage(any(), any(), any());
	}

	@Test
	void applyToMeasurementUsesBioimpedanceBeforeDeurenberg() {
		final AnthropometricMeasurement measurement = createMeasurementWithWeight(70.0, 1.75);
		final Bioimpedance bioimpedance = new Bioimpedance();
		bioimpedance.setBodyFatPercentage(24.0);
		measurement.setBioimpedance(bioimpedance);

		service.applyToMeasurement(measurement, paciente, 22.86);

		assertThat(measurement.getPorcentajeGrasaCorporal()).isEqualTo(24.0);
		assertThat(measurement.getIndiceGrasaCorporal()).isEqualTo(24.0);
		assertThat(measurement.getPorcentajeMasaMuscular()).isEqualTo(76.0);
		assertThat(measurement.getMetodoObtencion()).isEqualTo(MetodoObtencionComposicionCorporal.BIOIMPEDANCIA);
		verify(bodyFatCalculatorService, never()).calculateBodyFatPercentage(any(), any(), any());
	}

	@Test
	void applyToMeasurementUsesSkinfoldsBeforeDeurenberg() {
		final AnthropometricMeasurement measurement = createMeasurementWithWeight(70.0, 1.75);
		final Skinfolds skinfolds = new Skinfolds();
		skinfolds.setPectoralSkinfold(10.0);
		skinfolds.setAbdominalSkinfold(15.0);
		skinfolds.setFrontalThighSkinfold(12.0);
		measurement.setSkinfolds(skinfolds);
		when(bodyFatCalculatorService.calculateBodyFatFromSkinfolds(10.0, 15.0, 12.0, 30, "M")).thenReturn(19.0);

		service.applyToMeasurement(measurement, paciente, 22.86);

		assertThat(measurement.getPorcentajeGrasaCorporal()).isEqualTo(19.0);
		assertThat(measurement.getPorcentajeMasaMuscular()).isEqualTo(80.0);
		assertThat(measurement.getMetodoObtencion()).isEqualTo(MetodoObtencionComposicionCorporal.PLIEGUES);
		verify(bodyFatCalculatorService).calculateBodyFatFromSkinfolds(10.0, 15.0, 12.0, 30, "M");
		verify(bodyFatCalculatorService, never()).calculateBodyFatPercentage(any(), any(), any());
	}

	@Test
	void applyToMeasurementUsesDeurenbergWhenNoOtherSourceAvailable() {
		final AnthropometricMeasurement measurement = createMeasurementWithWeight(70.0, 1.75);
		when(bodyFatCalculatorService.calculateBodyFatPercentage(eq(22.86), eq(30), eq("M"))).thenReturn(16.5);

		service.applyToMeasurement(measurement, paciente, 22.86);

		assertThat(measurement.getPorcentajeGrasaCorporal()).isEqualTo(16.5);
		assertThat(measurement.getIndiceGrasaCorporal()).isEqualTo(16.5);
		assertThat(measurement.getPorcentajeMasaMuscular()).isEqualTo(80.0);
		assertThat(measurement.getMetodoObtencion()).isEqualTo(MetodoObtencionComposicionCorporal.DEURENBERG);
		verify(bodyFatCalculatorService).calculateBodyFatPercentage(22.86, 30, "M");
	}

	@Test
	void applyToMeasurementKeepsManualMusclePercentage() {
		final AnthropometricMeasurement measurement = createMeasurementWithWeight(70.0, 1.75);
		measurement.setPorcentajeGrasaCorporal(20.0);
		measurement.setPorcentajeMasaMuscular(42.0);

		service.applyToMeasurement(measurement, paciente, 22.86);

		assertThat(measurement.getPorcentajeMasaMuscular()).isEqualTo(42.0);
	}

	@Test
	void applyToMeasurementDoesNothingWhenNoInputsAvailable() {
		final AnthropometricMeasurement measurement = new AnthropometricMeasurement();

		service.applyToMeasurement(measurement, paciente, null);

		assertThat(measurement.getBodyComposition()).isNotNull();
		assertThat(measurement.getPorcentajeGrasaCorporal()).isNull();
		assertThat(measurement.getIndiceGrasaCorporal()).isNull();
		assertThat(measurement.getPorcentajeMasaMuscular()).isNull();
		assertThat(measurement.getMetodoObtencion()).isNull();
	}

	@Test
	void applyToMeasurementPreservesDexaOverrideWhenBioimpedanceRecalculatesFat() {
		final AnthropometricMeasurement measurement = createMeasurementWithWeight(70.0, 1.75);
		final Bioimpedance bioimpedance = new Bioimpedance();
		bioimpedance.setBodyFatPercentage(24.0);
		measurement.setBioimpedance(bioimpedance);
		measurement.setMetodoObtencion(MetodoObtencionComposicionCorporal.DEXA);

		service.applyToMeasurement(measurement, paciente, 22.86);

		assertThat(measurement.getPorcentajeGrasaCorporal()).isEqualTo(24.0);
		assertThat(measurement.getMetodoObtencion()).isEqualTo(MetodoObtencionComposicionCorporal.DEXA);
	}

	@Test
	void applyToMeasurementPreservesOtroOverrideWithManualFatEntry() {
		final AnthropometricMeasurement measurement = createMeasurementWithWeight(70.0, 1.75);
		measurement.setPorcentajeGrasaCorporal(21.0);
		measurement.setMetodoObtencion(MetodoObtencionComposicionCorporal.OTRO);

		service.applyToMeasurement(measurement, paciente, 22.86);

		assertThat(measurement.getPorcentajeGrasaCorporal()).isEqualTo(21.0);
		assertThat(measurement.getMetodoObtencion()).isEqualTo(MetodoObtencionComposicionCorporal.OTRO);
	}

	@Test
	void applyToMeasurementConsolidatesBoneMassFromBioimpedance() {
		final AnthropometricMeasurement measurement = createMeasurementWithWeight(70.0, 1.75);
		final Bioimpedance bioimpedance = new Bioimpedance();
		bioimpedance.setBoneMass(2.8);
		bioimpedance.setBoneMassPercentage(4.0);
		measurement.setBioimpedance(bioimpedance);

		service.applyToMeasurement(measurement, paciente, 22.86);

		assertThat(measurement.getMasaOseaKg()).isEqualTo(2.8);
		assertThat(measurement.getPorcentajeMasaOsea()).isEqualTo(4.0);
	}

	@Test
	void applyToMeasurementPrefersBioimpedanceBoneMassOverManual() {
		final AnthropometricMeasurement measurement = createMeasurementWithWeight(70.0, 1.75);
		measurement.setMasaOseaKg(3.0);
		measurement.setPorcentajeMasaOsea(5.0);
		final Bioimpedance bioimpedance = new Bioimpedance();
		bioimpedance.setBoneMass(2.5);
		bioimpedance.setBoneMassPercentage(3.6);
		measurement.setBioimpedance(bioimpedance);

		service.applyToMeasurement(measurement, paciente, 22.86);

		assertThat(measurement.getMasaOseaKg()).isEqualTo(2.5);
		assertThat(measurement.getPorcentajeMasaOsea()).isEqualTo(3.6);
	}

	@Test
	void applyToMeasurementDerivesBoneMassPercentageFromKg() {
		final AnthropometricMeasurement measurement = createMeasurementWithWeight(70.0, 1.75);
		measurement.setMasaOseaKg(3.5);

		service.applyToMeasurement(measurement, paciente, 22.86);

		assertThat(measurement.getMasaOseaKg()).isEqualTo(3.5);
		assertThat(measurement.getPorcentajeMasaOsea()).isEqualTo(5.0);
	}

	@Test
	void applyToMeasurementCalculatesMusclePercentageWithBoneAndWater() {
		final AnthropometricMeasurement measurement = createMeasurementWithWeight(70.0, 1.75);
		measurement.setPorcentajeGrasaCorporal(24.0);
		measurement.setMasaOseaKg(2.8);
		measurement.setPorcentajeMasaOsea(4.0);
		final Bioimpedance bioimpedance = new Bioimpedance();
		bioimpedance.setTotalBodyWaterPercentage(55.0);
		measurement.setBioimpedance(bioimpedance);

		service.applyToMeasurement(measurement, paciente, 22.86);

		assertThat(measurement.getPorcentajeMasaMuscular()).isEqualTo(20.0);
	}

	@Test
	void applyToMeasurementCalculatesMusclePercentageWithBoneOnly() {
		final AnthropometricMeasurement measurement = createMeasurementWithWeight(70.0, 1.75);
		measurement.setPorcentajeGrasaCorporal(20.0);
		measurement.setPorcentajeMasaOsea(4.0);

		service.applyToMeasurement(measurement, paciente, 22.86);

		assertThat(measurement.getPorcentajeMasaMuscular()).isEqualTo(76.0);
	}

	private AnthropometricMeasurement createMeasurementWithWeight(final Double weight, final Double height) {
		final AnthropometricMeasurement measurement = new AnthropometricMeasurement();
		final BodyMass bodyMass = new BodyMass();
		bodyMass.setWeight(weight);
		bodyMass.setHeight(height);
		measurement.setBodyMass(bodyMass);
		return measurement;
	}

}
