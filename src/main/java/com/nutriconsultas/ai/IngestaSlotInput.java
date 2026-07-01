package com.nutriconsultas.ai;

import java.util.List;

import org.springframework.lang.Nullable;

/**
 * Meal slot in a menu or diet plan draft.
 */
public record IngestaSlotInput(@Nullable String nombre, @Nullable Integer orden, List<IngestaSlotItemInput> items) {
}
