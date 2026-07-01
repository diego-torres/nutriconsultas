package com.nutriconsultas.ai;

import java.util.List;

/**
 * Multi-day diet plan payload for plan validation.
 */
public record DietPlanInput(List<DietPlanDayInput> days) {
}
