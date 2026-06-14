package com.nutriconsultas.paciente.calculation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TdeeCalculationServiceTest {

	@Test
	void calculateGetValid() {
		final Double result = TdeeCalculationService.calculateGet(1500.0, 1.55);
		assertThat(result).isEqualTo(2325.0);
	}

	@Test
	void calculateGetNullBmr() {
		assertThat(TdeeCalculationService.calculateGet(null, 1.55)).isNull();
	}

	@Test
	void calculateGetNullFactor() {
		assertThat(TdeeCalculationService.calculateGet(1500.0, null)).isNull();
	}

	@Test
	void resolveHarrisBenedictModerateFactor() {
		final Double factor = TdeeCalculationService.resolveActivityFactor(ActivityFactorScale.HARRIS_BENEDICT,
				PhysicalActivityLevel.MODERATE, null, null);
		assertThat(factor).isEqualTo(1.55);
	}

	@Test
	void resolveFaoWhoIntenseFactor() {
		final Double factor = TdeeCalculationService.resolveActivityFactor(ActivityFactorScale.FAO_WHO,
				PhysicalActivityLevel.INTENSE, null, null);
		assertThat(factor).isEqualTo(2.40);
	}

	@Test
	void resolveOmsLightFactor() {
		final Double factor = TdeeCalculationService.resolveActivityFactor(ActivityFactorScale.OMS,
				PhysicalActivityLevel.LIGHT, null, null);
		assertThat(factor).isEqualTo(1.60);
	}

	@Test
	void resolveCustomScaleUsesPatientFactors() {
		final CustomActivityFactors custom = new CustomActivityFactors(1.25, 1.40, 1.60, 1.80, 2.0);
		final Double factor = TdeeCalculationService.resolveActivityFactor(ActivityFactorScale.CUSTOM,
				PhysicalActivityLevel.MODERATE, custom, null);
		assertThat(factor).isEqualTo(1.60);
	}

	@Test
	void resolveCustomLevelUsesExplicitFactor() {
		final Double factor = TdeeCalculationService.resolveActivityFactor(ActivityFactorScale.HARRIS_BENEDICT,
				PhysicalActivityLevel.CUSTOM, null, 1.42);
		assertThat(factor).isEqualTo(1.42);
	}

	@Test
	void calculateGetFromInputsUsesSelectedBmrFormula() {
		final Double get = TdeeCalculationService.calculateGetFromInputs(BmrFormulaType.MIFFLIN_ST_JEOR, 70.0, 1.75, 30,
				true, ActivityFactorScale.HARRIS_BENEDICT, PhysicalActivityLevel.SEDENTARY, null, null);
		assertThat(get).isNotNull();
		assertThat(get).isGreaterThan(1500.0);
	}

	@Test
	void calculateBmrPromedioDefault() {
		final Double bmr = TdeeCalculationService.calculateBmr(null, 70.0, 1.75, 30, true);
		assertThat(bmr).isNotNull();
	}

}
