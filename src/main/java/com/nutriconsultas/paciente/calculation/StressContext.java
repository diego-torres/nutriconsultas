package com.nutriconsultas.paciente.calculation;

import java.util.Date;

import org.springframework.lang.Nullable;

/**
 * Resolved physiological stress configuration for energy calculations.
 */
public record StressContext(Boolean active, PhysiologicalStressType stressType, StressFormulaTable formulaTable,
		StressIncrementMode incrementMode, Double factorValue, Date validFrom, Date validUntil,
		Double feverTemperature) {

	public static StressContext inactive() {
		return new StressContext(false, null, null, null, null, null, null, null);
	}

	public static StressContext fromValues(final Boolean active, @Nullable final PhysiologicalStressType stressType,
			@Nullable final StressFormulaTable formulaTable, @Nullable final StressIncrementMode incrementMode,
			@Nullable final Double factorValue, @Nullable final Date validFrom, @Nullable final Date validUntil,
			@Nullable final Double feverTemperature) {
		return new StressContext(active, stressType, formulaTable, incrementMode, factorValue, validFrom, validUntil,
				feverTemperature);
	}

}
