package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nutriconsultas.alimentos.AlimentosRepository;

@ExtendWith(MockitoExtension.class)
class ValidatePlanConstraintsToolServiceTest {

	private static final String NUTRITIONIST_ID = "auth0|nutritionist-a";

	@InjectMocks
	private ValidatePlanConstraintsToolServiceImpl service;

	@Mock
	private AiIngestaNutrientCalculator ingestaNutrientCalculator;

	@Mock
	private AlimentosRepository alimentosRepository;

	@Test
	void validateReturnsValidWhenConstraintsMet() {
		final NutrientSummary nutrients = new NutrientSummary(2000, 130.0, 70.0, 200.0, 25.0, 1500.0, 3000.0);
		when(ingestaNutrientCalculator.computeIngestas(eq(NUTRITIONIST_ID), any())).thenReturn(AiToolResult
			.success(new AiIngestaNutrientCalculator.IngestaNutrientComputation(nutrients, List.of(), Set.of(1L))));

		final ValidatePlanConstraintsRequest request = new ValidatePlanConstraintsRequest(AiPlanType.MENU,
				new MenuPlanInput(List.of(new IngestaSlotInput("Desayuno", 1,
						List.of(new IngestaSlotItemInput("ALIMENTO", null, 1L, 1, null))))),
				null, null, 2000.0, 120.0, null, null, 2000.0, null, 50.0);

		final AiToolResult<PlanConstraintValidationData> result = service.validate(NUTRITIONIST_ID, request, null);

		assertThat(result.success()).isTrue();
		assertThat(result.data().valid()).isTrue();
		assertThat(result.data().computedNutrients().energiaKcal()).isEqualTo(2000);
	}

	@Test
	void validateReturnsInvalidWhenKcalFarFromTarget() {
		final NutrientSummary nutrients = new NutrientSummary(2600, 130.0, 70.0, 200.0, 25.0, 1500.0, 3000.0);
		when(ingestaNutrientCalculator.computeIngestas(eq(NUTRITIONIST_ID), any())).thenReturn(AiToolResult
			.success(new AiIngestaNutrientCalculator.IngestaNutrientComputation(nutrients, List.of(), Set.of())));

		final ValidatePlanConstraintsRequest request = new ValidatePlanConstraintsRequest(AiPlanType.MENU,
				new MenuPlanInput(List.of(new IngestaSlotInput("Comida", 1, List.of()))), null, null, 2000.0, null,
				null, null, null, null, 50.0);

		final AiToolResult<PlanConstraintValidationData> result = service.validate(NUTRITIONIST_ID, request, null);

		assertThat(result.success()).isTrue();
		assertThat(result.data().valid()).isFalse();
		assertThat(result.data().warnings()).anyMatch(w -> w.code() == PlanConstraintWarningCode.KCAL_OUT_OF_RANGE);
	}

	@Test
	void validateRejectsMissingMenuForMenuPlanType() {
		final ValidatePlanConstraintsRequest request = new ValidatePlanConstraintsRequest(AiPlanType.MENU, null, null,
				null, null, null, null, null, null, null, null);

		final AiToolResult<PlanConstraintValidationData> result = service.validate(NUTRITIONIST_ID, request, null);

		assertThat(result.success()).isFalse();
		assertThat(result.errorCode()).isEqualTo(AiToolErrorCode.VALIDATION);
	}

	@Test
	void validateDelegatesDishComputation() {
		final NutrientSummary nutrients = new NutrientSummary(400, 30.0, 10.0, 40.0, 5.0, 300.0, 500.0);
		when(ingestaNutrientCalculator.computeDish(eq(NUTRITIONIST_ID), any())).thenReturn(AiToolResult
			.success(new AiIngestaNutrientCalculator.IngestaNutrientComputation(nutrients, List.of(), Set.of(2L))));

		final ValidatePlanConstraintsRequest request = new ValidatePlanConstraintsRequest(AiPlanType.DISH, null, null,
				new DishPlanInput(List.of(new RecipeIngredientInput(2L, "1", null, null)), 1), null, null, null, null,
				null, null, null);

		final AiToolResult<PlanConstraintValidationData> result = service.validate(NUTRITIONIST_ID, request, null);

		assertThat(result.success()).isTrue();
		assertThat(result.data().computedNutrients().energiaKcal()).isEqualTo(400);
	}

}
