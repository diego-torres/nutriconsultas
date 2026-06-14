package com.nutriconsultas.paciente.calculation;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.nutriconsultas.paciente.Paciente;

/**
 * Catalog of default stress multipliers by clinical reference table and stress type.
 *
 * <p>
 * Values are documented clinical approximations (Long, ASPEN) for nutrition planning.
 */
public final class PhysiologicalStressCatalog {

	/** Increment per °C above 37 °C applied to BMR (fever formula). */
	public static final double FEVER_INCREMENT_PER_DEGREE = 0.13;

	private PhysiologicalStressCatalog() {
		// Utility class
	}

	/**
	 * Returns the default multiplier for a stress type and reference table, or {@code null}
	 * when the nutritionist must supply a custom value.
	 */
	public static Double defaultMultiplier(final PhysiologicalStressType stressType,
			final StressFormulaTable formulaTable) {
		if (stressType == null || stressType == PhysiologicalStressType.NONE
				|| stressType == PhysiologicalStressType.OTHER) {
			return null;
		}
		if (formulaTable == StressFormulaTable.FEVER_PER_DEGREE && stressType == PhysiologicalStressType.FEVER) {
			return null;
		}
		if (formulaTable == StressFormulaTable.CUSTOM) {
			return null;
		}
		final StressFormulaTable table = formulaTable != null ? formulaTable : StressFormulaTable.LONG;
		return switch (table) {
			case LONG -> longMultiplier(stressType);
			case ASPEN -> aspenMultiplier(stressType);
			case FEVER_PER_DEGREE, CUSTOM -> null;
		};
	}

	/**
	 * Suggests stress types based on pathology flags already captured on the patient record.
	 */
	public static List<PhysiologicalStressType> suggestFromPathologies(final Paciente paciente) {
		if (paciente == null) {
			return List.of();
		}
		final Set<PhysiologicalStressType> suggestions = new LinkedHashSet<>();
		if (Boolean.TRUE.equals(paciente.getDiabetes())) {
			suggestions.add(PhysiologicalStressType.MODERATE_INFECTION);
		}
		if (Boolean.TRUE.equals(paciente.getEnfermedadesHepaticas())) {
			suggestions.add(PhysiologicalStressType.ORGAN_FAILURE);
		}
		if (Boolean.TRUE.equals(paciente.getHipertension())) {
			suggestions.add(PhysiologicalStressType.ORGAN_FAILURE);
		}
		if (Boolean.TRUE.equals(paciente.getAnemia())) {
			suggestions.add(PhysiologicalStressType.MODERATE_INFECTION);
		}
		if (Boolean.TRUE.equals(paciente.getObesidad())) {
			suggestions.add(PhysiologicalStressType.POST_OPERATIVE);
		}
		if (Boolean.TRUE.equals(paciente.getPregnancy())) {
			suggestions.add(PhysiologicalStressType.PREGNANCY_COMPLICATION);
		}
		if (Boolean.TRUE.equals(paciente.getBulimia()) || Boolean.TRUE.equals(paciente.getAnorexia())) {
			suggestions.add(PhysiologicalStressType.OTHER);
		}
		suggestions.remove(PhysiologicalStressType.NONE);
		return new ArrayList<>(suggestions);
	}

	public static List<PhysiologicalStressType> commonTypes() {
		return EnumSet.allOf(PhysiologicalStressType.class)
			.stream()
			.filter(PhysiologicalStressType::isCommon)
			.filter(type -> type != PhysiologicalStressType.NONE)
			.toList();
	}

	public static List<PhysiologicalStressType> uncommonTypes() {
		return EnumSet.allOf(PhysiologicalStressType.class)
			.stream()
			.filter(type -> !type.isCommon())
			.filter(type -> type != PhysiologicalStressType.NONE)
			.toList();
	}

	private static Double longMultiplier(final PhysiologicalStressType stressType) {
		return switch (stressType) {
			case FEVER -> null;
			case MINOR_SURGERY -> 1.10;
			case MAJOR_SURGERY -> 1.20;
			case MODERATE_INFECTION -> 1.20;
			case SEVERE_INFECTION -> 1.40;
			case SEPSIS -> 1.50;
			case TRAUMA -> 1.35;
			case BURNS -> 1.50;
			case CANCER -> 1.25;
			case FRACTURE -> 1.20;
			case POST_OPERATIVE -> 1.15;
			case MULTIPLE_TRAUMA -> 1.50;
			case HEAD_INJURY -> 1.40;
			case ORGAN_FAILURE -> 1.50;
			case PREGNANCY_COMPLICATION -> 1.20;
			case COPD_EXACERBATION -> 1.20;
			default -> null;
		};
	}

	private static Double aspenMultiplier(final PhysiologicalStressType stressType) {
		return switch (stressType) {
			case FEVER -> null;
			case MINOR_SURGERY -> 1.10;
			case MAJOR_SURGERY -> 1.25;
			case MODERATE_INFECTION -> 1.30;
			case SEVERE_INFECTION -> 1.55;
			case SEPSIS -> 1.60;
			case TRAUMA -> 1.40;
			case BURNS -> 1.80;
			case CANCER -> 1.30;
			case FRACTURE -> 1.20;
			case POST_OPERATIVE -> 1.15;
			case MULTIPLE_TRAUMA -> 1.60;
			case HEAD_INJURY -> 1.45;
			case ORGAN_FAILURE -> 1.55;
			case PREGNANCY_COMPLICATION -> 1.25;
			case COPD_EXACERBATION -> 1.25;
			default -> null;
		};
	}

}
