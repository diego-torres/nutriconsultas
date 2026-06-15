package com.nutriconsultas.paciente.calculation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PhysiologicalStressCalculationServiceTest {

	@Test
	void calculateLongMultiplierStressOnBmr() {
		final StressContext context = StressContext.fromValues(true, PhysiologicalStressType.MAJOR_SURGERY,
				StressFormulaTable.LONG, StressIncrementMode.MULTIPLIER_BMR, null, null, null, null);
		final Double stressKcal = PhysiologicalStressCalculationService.calculateStressKcal(context, 1500.0, 2000.0,
				null);
		assertThat(stressKcal).isCloseTo(300.0, org.assertj.core.data.Offset.offset(0.01));
	}

	@Test
	void calculateAspenMultiplierStressOnGet() {
		final StressContext context = StressContext.fromValues(true, PhysiologicalStressType.TRAUMA,
				StressFormulaTable.ASPEN, StressIncrementMode.MULTIPLIER_GET, null, null, null, null);
		final Double stressKcal = PhysiologicalStressCalculationService.calculateStressKcal(context, 1500.0, 2000.0,
				null);
		assertThat(stressKcal).isCloseTo(800.0, org.assertj.core.data.Offset.offset(0.01));
	}

	@Test
	void calculateFixedKcalStress() {
		final StressContext context = StressContext.fromValues(true, PhysiologicalStressType.OTHER,
				StressFormulaTable.CUSTOM, StressIncrementMode.FIXED_KCAL, 450.0, null, null, null);
		assertThat(PhysiologicalStressCalculationService.calculateStressKcal(context, 1500.0, 2000.0, null))
			.isEqualTo(450.0);
	}

	@Test
	void calculateFeverPerDegreeStress() {
		final StressContext context = StressContext.fromValues(true, PhysiologicalStressType.FEVER,
				StressFormulaTable.FEVER_PER_DEGREE, StressIncrementMode.MULTIPLIER_BMR, null, null, null, 39.0);
		final Double stressKcal = PhysiologicalStressCalculationService.calculateStressKcal(context, 1500.0, 2000.0,
				null);
		assertThat(stressKcal).isEqualTo(390.0);
	}

	@Test
	void calculateStressUsesCustomFactorOverride() {
		final StressContext context = StressContext.fromValues(true, PhysiologicalStressType.OTHER,
				StressFormulaTable.LONG, StressIncrementMode.MULTIPLIER_BMR, 1.50, null, null, null);
		assertThat(PhysiologicalStressCalculationService.calculateStressKcal(context, 1000.0, 1500.0, null))
			.isEqualTo(500.0);
	}

	@Test
	void calculateFinalTotalAddsStressToAdjustedTotal() {
		assertThat(PhysiologicalStressCalculationService.calculateFinalTotalKcal(2200.0, 300.0)).isEqualTo(2500.0);
	}

	@Test
	void inactiveStressReturnsNull() {
		final StressContext context = StressContext.fromValues(false, PhysiologicalStressType.FEVER,
				StressFormulaTable.LONG, StressIncrementMode.MULTIPLIER_BMR, null, null, null, null);
		assertThat(PhysiologicalStressCalculationService.calculateStressKcal(context, 1500.0, 2000.0, null)).isNull();
	}

}
