package com.nutriconsultas.dieta;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-ingesta nutrient rollup for the diet full-nutrients modal.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngestaNutrientSummary {

	private Long id;

	private String name;

	private DietaNutrientTotals totals;

}
