package com.nutriconsultas.clinical.exam.anthropometric;

import com.nutriconsultas.paciente.NivelPeso;

/**
 * BMI and weight-level calculation for anthropometric measurements (#242).
 */
public final class AnthropometricBmiCalculator {

	private AnthropometricBmiCalculator() {
	}

	public static Double calculateImc(final Double weightKg, final Double heightMeters) {
		if (weightKg == null || heightMeters == null || heightMeters <= 0.0d) {
			return null;
		}
		return weightKg / Math.pow(heightMeters, 2);
	}

	public static NivelPeso calculateNivelPeso(final Double imc) {
		if (imc == null) {
			return null;
		}
		if (imc > 30.0d) {
			return NivelPeso.SOBREPESO;
		}
		if (imc > 25.0d) {
			return NivelPeso.ALTO;
		}
		if (imc > 18.5d) {
			return NivelPeso.NORMAL;
		}
		return NivelPeso.BAJO;
	}

}
