package com.nutriconsultas.ai;

import org.springframework.lang.Nullable;

/**
 * Macros and common micros for AI nutrient tools ({@code docs/ai/TOOL-CONTRACT.md}).
 */
public record NutrientSummary(@Nullable Integer energiaKcal, @Nullable Double proteinaG, @Nullable Double lipidosG,
		@Nullable Double hidratosDeCarbonoG, @Nullable Double fibraG, @Nullable Double sodioMg,
		@Nullable Double potasioMg) {
}
