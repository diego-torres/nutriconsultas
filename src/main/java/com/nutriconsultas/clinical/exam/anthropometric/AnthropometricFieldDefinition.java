package com.nutriconsultas.clinical.exam.anthropometric;

import java.util.Set;

/**
 * Metadata for one correctable anthropometric field (#242).
 */
public record AnthropometricFieldDefinition(String fieldKey, String label, String unit, Double minValue,
		Double maxValue, Set<AnthropometricRecalcGroup> recalcGroups, boolean confirmDerivedRecalc) {
}
