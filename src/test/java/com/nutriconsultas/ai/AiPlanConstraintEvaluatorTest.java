package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.alimentos.AlimentosRepository;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiPlanConstraintEvaluatorTest {

	@Test
	void evaluateFlagsKcalOutOfRange() {
		final NutrientSummary computed = new NutrientSummary(2200, 120.0, 70.0, 200.0, 25.0, 1800.0, 3000.0);
		final ValidatePlanConstraintsRequest request = new ValidatePlanConstraintsRequest(AiPlanType.MENU, null, null,
				null, 2000.0, null, null, null, null, null, 50.0);

		final List<PlanConstraintWarning> warnings = AiPlanConstraintEvaluator.evaluate(computed, request, null,
				Set.of(), mock(AlimentosRepository.class));

		assertThat(warnings).anyMatch(warning -> warning.code() == PlanConstraintWarningCode.KCAL_OUT_OF_RANGE);
	}

	@Test
	void evaluateFlagsProteinLow() {
		final NutrientSummary computed = new NutrientSummary(1800, 90.0, 60.0, 180.0, 20.0, 1500.0, 2800.0);
		final ValidatePlanConstraintsRequest request = new ValidatePlanConstraintsRequest(AiPlanType.MENU, null, null,
				null, null, 120.0, null, null, null, null, null);

		final List<PlanConstraintWarning> warnings = AiPlanConstraintEvaluator.evaluate(computed, request, null,
				Set.of(), mock(AlimentosRepository.class));

		assertThat(warnings).anyMatch(warning -> warning.code() == PlanConstraintWarningCode.PROTEIN_LOW);
	}

	@Test
	void evaluateFlagsSodiumHigh() {
		final NutrientSummary computed = new NutrientSummary(1800, 120.0, 60.0, 180.0, 20.0, 2500.0, 2800.0);
		final ValidatePlanConstraintsRequest request = new ValidatePlanConstraintsRequest(AiPlanType.MENU, null, null,
				null, null, null, null, null, 2000.0, null, null);

		final List<PlanConstraintWarning> warnings = AiPlanConstraintEvaluator.evaluate(computed, request, null,
				Set.of(), mock(AlimentosRepository.class));

		assertThat(warnings).anyMatch(warning -> warning.code() == PlanConstraintWarningCode.SODIUM_HIGH);
	}

	@Test
	void evaluateUsesPatientKcalTargetWhenStressActive() {
		final NutrientSummary computed = new NutrientSummary(2300, 120.0, 60.0, 180.0, 20.0, 1500.0, 2800.0);
		final ValidatePlanConstraintsRequest request = new ValidatePlanConstraintsRequest(AiPlanType.MENU, null, null,
				null, null, null, null, null, null, null, 50.0);
		final AiPatientPromptContext patientContext = new AiPatientPromptContext(1L, 2000.0, 2200.0, true, "F", false,
				null, null, Map.of(), null, null);

		final List<PlanConstraintWarning> warnings = AiPlanConstraintEvaluator.evaluate(computed, request,
				patientContext, Set.of(), mock(AlimentosRepository.class));

		assertThat(warnings).anyMatch(warning -> warning.code() == PlanConstraintWarningCode.KCAL_OUT_OF_RANGE);
		assertThat(AiPlanConstraintEvaluator.patientContextApplied(patientContext, request, warnings)).isTrue();
	}

	@Test
	void evaluateFlagsAllergyRiskFromPatientContext() {
		final AlimentosRepository repository = mock(AlimentosRepository.class);
		final Alimento mani = new Alimento();
		mani.setId(5L);
		mani.setNombreAlimento("Crema de cacahuate");
		mani.setClasificacion("Grasas");
		when(repository.findById(5L)).thenReturn(java.util.Optional.of(mani));

		final AiPatientPromptContext patientContext = new AiPatientPromptContext(1L, 2000.0, null, false, "F", false,
				null, null, Map.of(), "cacahuate, mariscos", null);
		final ValidatePlanConstraintsRequest request = new ValidatePlanConstraintsRequest(AiPlanType.DISH, null, null,
				null, null, null, null, null, null, null, null);

		final List<PlanConstraintWarning> warnings = AiPlanConstraintEvaluator.evaluate(
				new NutrientSummary(500, 20.0, 30.0, 10.0, 5.0, 200.0, 300.0), request, patientContext, Set.of(5L),
				repository);

		assertThat(warnings).anyMatch(warning -> warning.code() == PlanConstraintWarningCode.ALLERGY_RISK
				&& warning.severity() == PlanConstraintWarningSeverity.ERROR);
	}

	@Test
	void evaluateAddsPathologyInfoNote() {
		final AiPatientPromptContext patientContext = new AiPatientPromptContext(1L, 2000.0, null, false, "F", false,
				null, null, Map.of("diabetes", true), null, null);
		final ValidatePlanConstraintsRequest request = new ValidatePlanConstraintsRequest(AiPlanType.MENU, null, null,
				null, null, null, null, null, null, null, null);

		final List<PlanConstraintWarning> warnings = AiPlanConstraintEvaluator.evaluate(
				new NutrientSummary(1800, 100.0, 50.0, 150.0, 20.0, 1200.0, 2500.0), request, patientContext, Set.of(),
				mock(AlimentosRepository.class));

		assertThat(warnings).anyMatch(warning -> warning.code() == PlanConstraintWarningCode.PATHOLOGY_NOTE
				&& warning.severity() == PlanConstraintWarningSeverity.INFO);
	}

}
