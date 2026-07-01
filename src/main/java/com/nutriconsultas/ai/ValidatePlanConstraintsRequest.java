package com.nutriconsultas.ai;

import java.util.List;

import org.springframework.lang.Nullable;

/**
 * Input for {@code validate_plan_constraints}.
 */
public record ValidatePlanConstraintsRequest(AiPlanType planType, @Nullable MenuPlanInput menu,
		@Nullable DietPlanInput dietPlan, @Nullable DishPlanInput dish, @Nullable Double targetKcal,
		@Nullable Double targetProteinaG, @Nullable Double targetLipidosG, @Nullable Double targetHidratosG,
		@Nullable Double maxSodioMg, @Nullable List<Long> excludedAlimentoIds, @Nullable Double toleranceKcal) {
}
