package com.nutriconsultas.paciente.calculation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-patient custom activity factors when {@link ActivityFactorScale#CUSTOM} is
 * selected.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomActivityFactors {

	private Double sedentary;

	private Double light;

	private Double moderate;

	private Double intense;

	private Double veryIntense;

	public Double forLevel(final PhysicalActivityLevel level) {
		if (level == null) {
			return null;
		}
		return switch (level) {
			case SEDENTARY -> sedentary;
			case LIGHT -> light;
			case MODERATE -> moderate;
			case INTENSE -> intense;
			case VERY_INTENSE -> veryIntense;
			case CUSTOM -> null;
		};
	}

}
