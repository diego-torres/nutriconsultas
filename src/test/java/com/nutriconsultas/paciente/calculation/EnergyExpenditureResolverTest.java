package com.nutriconsultas.paciente.calculation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nutriconsultas.paciente.Paciente;

class EnergyExpenditureResolverTest {

	private Paciente paciente;

	@BeforeEach
	void setUp() {
		paciente = new Paciente();
		paciente.setDob(new Date(System.currentTimeMillis() - 30L * 365 * 24 * 60 * 60 * 1000));
		paciente.setGender("M");
		paciente.setTefMethod(TefMethod.FIXED);
		paciente.setTefBase(TefBase.GET);
		paciente.setTefFixedPercent(10.0);
	}

	@Test
	void resolveIncludesTefAndTotalAdjusted() {
		final EnergyExpenditureResolver.EnergyResult result = EnergyExpenditureResolver.resolve(paciente,
				BmrFormulaType.MIFFLIN_ST_JEOR, PhysicalActivityLevel.MODERATE, null, 70.0, 1.75);

		assertThat(result.bmr()).isNotNull();
		assertThat(result.getKcal()).isNotNull();
		assertThat(result.tefKcal()).isNotNull();
		assertThat(result.totalAdjustedKcal()).isEqualTo(result.getKcal() + result.tefKcal());
		assertThat(result.finalTotalKcal()).isEqualTo(result.totalAdjustedKcal());
		assertThat(result.activityKcal()).isEqualTo(result.getKcal() - result.bmr());
	}

	@Test
	void resolveIncludesStressWhenPatientStressActive() {
		paciente.setPhysiologicalStressActive(true);
		paciente.setPhysiologicalStressType(PhysiologicalStressType.MINOR_SURGERY);
		paciente.setStressFormulaTable(StressFormulaTable.LONG);
		paciente.setStressIncrementMode(StressIncrementMode.MULTIPLIER_BMR);

		final EnergyExpenditureResolver.EnergyResult result = EnergyExpenditureResolver.resolve(paciente,
				BmrFormulaType.MIFFLIN_ST_JEOR, PhysicalActivityLevel.MODERATE, null, 70.0, 1.75);

		assertThat(result.stressKcal()).isNotNull().isPositive();
		assertThat(result.finalTotalKcal()).isEqualTo(result.totalAdjustedKcal() + result.stressKcal());
	}

	@Test
	void applyTefUsesMacronutrientPreferences() {
		paciente.setTefMethod(TefMethod.MACRONUTRIENTS);
		paciente.setTefMacroProteinPercent(40.0);
		paciente.setTefMacroCarbsPercent(40.0);
		paciente.setTefMacroFatPercent(20.0);

		final EnergyExpenditureResolver.EnergyResult result = EnergyExpenditureResolver.applyTef(paciente, 1500.0, 1.55,
				2325.0);

		assertThat(result.tefKcal()).isNotNull();
		assertThat(result.totalAdjustedKcal()).isGreaterThan(result.getKcal());
	}

	@Test
	void resolveTargetDailyCaloriesPrefersFinalTotal() {
		paciente.setGetKcal(2000.0);
		paciente.setTotalAdjustedKcal(2200.0);
		paciente.setFinalTotalKcal(2500.0);

		assertThat(EnergyExpenditureResolver.resolveTargetDailyCalories(paciente)).isEqualTo(2500.0);
	}

	@Test
	void resolveTargetDailyCaloriesPrefersTotalAdjusted() {
		paciente.setGetKcal(2000.0);
		paciente.setTotalAdjustedKcal(2200.0);

		assertThat(EnergyExpenditureResolver.resolveTargetDailyCalories(paciente)).isEqualTo(2200.0);
	}

	@Test
	void resolveTargetDailyCaloriesFallsBackToGet() {
		paciente.setGetKcal(2000.0);

		assertThat(EnergyExpenditureResolver.resolveTargetDailyCalories(paciente)).isEqualTo(2000.0);
	}

}
