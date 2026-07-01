package com.nutriconsultas.ai;

import java.util.List;

/**
 * One-day menu payload for plan validation.
 */
public record MenuPlanInput(List<IngestaSlotInput> ingestas) {
}
