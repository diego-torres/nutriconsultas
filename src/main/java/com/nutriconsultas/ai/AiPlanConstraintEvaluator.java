package com.nutriconsultas.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.alimentos.AlimentosRepository;

/**
 * Evaluates computed nutrients against plan constraints and patient context.
 */
public final class AiPlanConstraintEvaluator {

	private static final double DEFAULT_TOLERANCE_KCAL = 50.0;

	private AiPlanConstraintEvaluator() {
	}

	public static List<PlanConstraintWarning> evaluate(final NutrientSummary computed,
			final ValidatePlanConstraintsRequest request, @Nullable final AiPatientPromptContext patientContext,
			final Set<Long> alimentoIds, final AlimentosRepository alimentosRepository) {
		final List<PlanConstraintWarning> warnings = new ArrayList<>();
		final Double targetKcal = resolveTargetKcal(request, patientContext);
		final double tolerance = request.toleranceKcal() != null ? request.toleranceKcal() : DEFAULT_TOLERANCE_KCAL;
		evaluateKcal(computed, targetKcal, tolerance, warnings);
		evaluateProtein(computed, request.targetProteinaG(), warnings);
		evaluateSodium(computed, request.maxSodioMg(), warnings);
		evaluateExclusions(alimentoIds, request.excludedAlimentoIds(), alimentosRepository, warnings);
		evaluateAllergies(alimentoIds, patientContext, alimentosRepository, warnings);
		evaluatePathologyNotes(patientContext, warnings);
		return warnings;
	}

	public static boolean isValid(final List<PlanConstraintWarning> warnings) {
		return warnings.stream().noneMatch(warning -> warning.severity() == PlanConstraintWarningSeverity.ERROR);
	}

	public static boolean patientContextApplied(@Nullable final AiPatientPromptContext patientContext,
			final ValidatePlanConstraintsRequest request, final List<PlanConstraintWarning> warnings) {
		if (patientContext == null) {
			return false;
		}
		final boolean targetFromPatient = request.targetKcal() == null
				&& resolveTargetKcal(request, patientContext) != null;
		final boolean allergyWarning = warnings.stream()
			.anyMatch(warning -> warning.code() == PlanConstraintWarningCode.ALLERGY_RISK);
		final boolean pathologyNote = warnings.stream()
			.anyMatch(warning -> warning.code() == PlanConstraintWarningCode.PATHOLOGY_NOTE);
		return targetFromPatient || StringUtils.hasText(patientContext.alergias()) || allergyWarning || pathologyNote
				|| hasActivePathology(patientContext);
	}

	@Nullable
	private static Double resolveTargetKcal(final ValidatePlanConstraintsRequest request,
			@Nullable final AiPatientPromptContext patientContext) {
		if (request.targetKcal() != null) {
			return request.targetKcal();
		}
		if (patientContext == null) {
			return null;
		}
		if (Boolean.TRUE.equals(patientContext.physiologicalStressActive())
				&& patientContext.finalTotalKcal() != null) {
			return patientContext.finalTotalKcal();
		}
		return patientContext.requerimientoKcal();
	}

	private static void evaluateKcal(final NutrientSummary computed, @Nullable final Double targetKcal,
			final double tolerance, final List<PlanConstraintWarning> warnings) {
		if (targetKcal == null || computed.energiaKcal() == null) {
			return;
		}
		final double delta = Math.abs(computed.energiaKcal() - targetKcal);
		if (delta <= tolerance) {
			return;
		}
		final PlanConstraintWarningSeverity severity = delta > tolerance * 2 ? PlanConstraintWarningSeverity.ERROR
				: PlanConstraintWarningSeverity.WARNING;
		warnings.add(new PlanConstraintWarning(
				PlanConstraintWarningCode.KCAL_OUT_OF_RANGE, "Las kcal calculadas (" + computed.energiaKcal()
						+ ") están fuera del objetivo (" + targetKcal.intValue() + " ± " + (int) tolerance + ").",
				severity));
	}

	private static void evaluateProtein(final NutrientSummary computed, @Nullable final Double targetProteinaG,
			final List<PlanConstraintWarning> warnings) {
		if (targetProteinaG == null || computed.proteinaG() == null) {
			return;
		}
		if (computed.proteinaG() + 0.001 < targetProteinaG) {
			warnings.add(new PlanConstraintWarning(PlanConstraintWarningCode.PROTEIN_LOW,
					"La proteína calculada (" + formatDouble(computed.proteinaG())
							+ " g) está por debajo del objetivo (" + formatDouble(targetProteinaG) + " g).",
					PlanConstraintWarningSeverity.WARNING));
		}
	}

	private static void evaluateSodium(final NutrientSummary computed, @Nullable final Double maxSodioMg,
			final List<PlanConstraintWarning> warnings) {
		if (maxSodioMg == null || computed.sodioMg() == null) {
			return;
		}
		if (computed.sodioMg() > maxSodioMg) {
			warnings.add(new PlanConstraintWarning(
					PlanConstraintWarningCode.SODIUM_HIGH, "El sodio calculado (" + formatDouble(computed.sodioMg())
							+ " mg) supera el máximo permitido (" + formatDouble(maxSodioMg) + " mg).",
					PlanConstraintWarningSeverity.WARNING));
		}
	}

	private static void evaluateExclusions(final Set<Long> alimentoIds, @Nullable final List<Long> excludedAlimentoIds,
			final AlimentosRepository alimentosRepository, final List<PlanConstraintWarning> warnings) {
		if (excludedAlimentoIds == null || excludedAlimentoIds.isEmpty()) {
			return;
		}
		for (final Long excludedId : excludedAlimentoIds) {
			if (alimentoIds.contains(excludedId)) {
				final String name = alimentosRepository.findById(excludedId)
					.map(Alimento::getNombreAlimento)
					.orElse("ID " + excludedId);
				warnings.add(new PlanConstraintWarning(PlanConstraintWarningCode.ALLERGY_RISK,
						"El plan incluye un alimento excluido: " + name + ".", PlanConstraintWarningSeverity.ERROR));
			}
		}
	}

	private static void evaluateAllergies(final Set<Long> alimentoIds,
			@Nullable final AiPatientPromptContext patientContext, final AlimentosRepository alimentosRepository,
			final List<PlanConstraintWarning> warnings) {
		if (patientContext == null || !StringUtils.hasText(patientContext.alergias())) {
			return;
		}
		for (final Long alimentoId : alimentoIds) {
			alimentosRepository.findById(alimentoId).ifPresent(alimento -> {
				if (matchesAllergy(alimento.getNombreAlimento(), patientContext.alergias())) {
					warnings.add(new PlanConstraintWarning(PlanConstraintWarningCode.ALLERGY_RISK,
							"Posible alérgeno en el plan: " + alimento.getNombreAlimento() + ".",
							PlanConstraintWarningSeverity.ERROR));
				}
			});
		}
	}

	private static void evaluatePathologyNotes(@Nullable final AiPatientPromptContext patientContext,
			final List<PlanConstraintWarning> warnings) {
		if (!hasActivePathology(patientContext)) {
			return;
		}
		warnings.add(new PlanConstraintWarning(PlanConstraintWarningCode.PATHOLOGY_NOTE,
				"Revise el plan considerando las patologías registradas del paciente; esta validación no sustituye criterio clínico.",
				PlanConstraintWarningSeverity.INFO));
	}

	private static boolean hasActivePathology(@Nullable final AiPatientPromptContext patientContext) {
		if (patientContext == null || patientContext.pathologyFlags() == null) {
			return false;
		}
		return patientContext.pathologyFlags().entrySet().stream().anyMatch(Map.Entry::getValue);
	}

	private static boolean matchesAllergy(final String foodName, final String alergias) {
		if (!StringUtils.hasText(foodName) || !StringUtils.hasText(alergias)) {
			return false;
		}
		final String lowerName = foodName.toLowerCase(Locale.ROOT);
		for (final String term : alergias.split(",")) {
			final String trimmed = term.trim();
			if (StringUtils.hasText(trimmed) && lowerName.contains(trimmed.toLowerCase(Locale.ROOT))) {
				return true;
			}
		}
		return false;
	}

	private static String formatDouble(final double value) {
		if (Math.rint(value) == value) {
			return String.valueOf((long) value);
		}
		return String.format(Locale.ROOT, "%.1f", value);
	}

}
