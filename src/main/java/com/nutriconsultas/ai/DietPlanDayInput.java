package com.nutriconsultas.ai;

import java.util.List;

import org.springframework.lang.Nullable;

/**
 * Single day inside a multi-day diet plan draft.
 */
public record DietPlanDayInput(int dayIndex, @Nullable String label, List<IngestaSlotInput> ingestas) {
}
