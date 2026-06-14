package com.nutriconsultas.paciente.calculation;

import com.nutriconsultas.paciente.Paciente;

/**
 * Utility to build {@link CustomActivityFactors} from a {@link Paciente} entity.
 */
public final class PatientEnergyPreferences {

	private PatientEnergyPreferences() {
		// Utility class
	}

	public static CustomActivityFactors customFactorsFrom(final Paciente paciente) {
		if (paciente == null) {
			return new CustomActivityFactors();
		}
		return new CustomActivityFactors(paciente.getCustomFactorSedentary(), paciente.getCustomFactorLight(),
				paciente.getCustomFactorModerate(), paciente.getCustomFactorIntense(),
				paciente.getCustomFactorVeryIntense());
	}

	public static ActivityFactorScale resolveScale(final Paciente paciente) {
		if (paciente == null || paciente.getActivityFactorScale() == null) {
			return ActivityFactorScale.HARRIS_BENEDICT;
		}
		return paciente.getActivityFactorScale();
	}

	public static BmrFormulaType resolveBmrFormula(final Paciente paciente) {
		if (paciente == null || paciente.getPreferredBmrFormula() == null) {
			return BmrFormulaType.PROMEDIO;
		}
		return paciente.getPreferredBmrFormula();
	}

}
