package com.nutriconsultas.ai;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nutriconsultas.alimentos.AlimentosRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ValidatePlanConstraintsToolServiceImpl implements ValidatePlanConstraintsToolService {

	static final int MAX_DIET_DAYS = 14;

	static final double MIN_TARGET_KCAL = 500.0;

	static final double MAX_TARGET_KCAL = 10_000.0;

	private final AiIngestaNutrientCalculator ingestaNutrientCalculator;

	private final AlimentosRepository alimentosRepository;

	public ValidatePlanConstraintsToolServiceImpl(final AiIngestaNutrientCalculator ingestaNutrientCalculator,
			final AlimentosRepository alimentosRepository) {
		this.ingestaNutrientCalculator = ingestaNutrientCalculator;
		this.alimentosRepository = alimentosRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public AiToolResult<PlanConstraintValidationData> validate(@NonNull final String nutritionistId,
			@NonNull final ValidatePlanConstraintsRequest request,
			@Nullable final AiPatientPromptContext patientContext) {
		if (!StringUtils.hasText(nutritionistId)) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, "Sesión de nutriólogo no válida.");
		}
		if (request.planType() == null) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, "El tipo de plan es obligatorio.");
		}
		final AiToolResult<Void> requestValidation = validateRequestBounds(request);
		if (!requestValidation.success()) {
			return AiToolResult.error(Objects.requireNonNull(requestValidation.errorCode()),
					Objects.requireNonNull(requestValidation.message()));
		}

		final AiToolResult<ComputationBundle> computation = computePlanNutrients(nutritionistId, request);
		if (!computation.success()) {
			return AiToolResult.error(Objects.requireNonNull(computation.errorCode()),
					Objects.requireNonNull(computation.message()));
		}
		final ComputationBundle bundle = Objects.requireNonNull(computation.data());
		final List<PlanConstraintWarning> warnings = new ArrayList<>(bundle.computationWarnings());
		warnings.addAll(AiPlanConstraintEvaluator.evaluate(bundle.nutrients(), request, patientContext,
				bundle.alimentoIds(), alimentosRepository));
		final boolean valid = AiPlanConstraintEvaluator.isValid(warnings);
		final boolean patientContextApplied = AiPlanConstraintEvaluator.patientContextApplied(patientContext, request,
				warnings);
		final PlanConstraintValidationData data = new PlanConstraintValidationData(valid, bundle.nutrients(), warnings,
				patientContextApplied);
		if (log.isInfoEnabled()) {
			log.info("AI tool validate_plan_constraints valid={} warningCount={}", valid, warnings.size());
		}
		return AiToolResult.success(data);
	}

	private AiToolResult<ComputationBundle> computePlanNutrients(final String nutritionistId,
			final ValidatePlanConstraintsRequest request) {
		return switch (request.planType()) {
			case DISH -> computeDishPlan(nutritionistId, request.dish());
			case MENU -> computeMenuPlan(nutritionistId, request.menu());
			case DIET_PLAN -> computeDietPlan(nutritionistId, request.dietPlan());
		};
	}

	private AiToolResult<ComputationBundle> computeDishPlan(final String nutritionistId,
			@Nullable final DishPlanInput dish) {
		if (dish == null) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, "El platillo es obligatorio para este tipo de plan.");
		}
		final AiToolResult<AiIngestaNutrientCalculator.IngestaNutrientComputation> result = ingestaNutrientCalculator
			.computeDish(nutritionistId, dish);
		return mapComputation(result);
	}

	private AiToolResult<ComputationBundle> computeMenuPlan(final String nutritionistId,
			@Nullable final MenuPlanInput menu) {
		if (menu == null) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, "El menú es obligatorio para este tipo de plan.");
		}
		final AiToolResult<AiIngestaNutrientCalculator.IngestaNutrientComputation> result = ingestaNutrientCalculator
			.computeIngestas(nutritionistId, menu.ingestas());
		return mapComputation(result);
	}

	private AiToolResult<ComputationBundle> computeDietPlan(final String nutritionistId,
			@Nullable final DietPlanInput dietPlan) {
		if (dietPlan == null || dietPlan.days() == null || dietPlan.days().isEmpty()) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, "El plan debe incluir al menos un día.");
		}
		if (dietPlan.days().size() > MAX_DIET_DAYS) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION,
					"El plan no puede superar " + MAX_DIET_DAYS + " días.");
		}
		NutrientSummary averageDaily = AiNutrientToolSupport.emptyNutrientSummary();
		final List<PlanConstraintWarning> warnings = new ArrayList<>();
		final Set<Long> alimentoIds = new HashSet<>();
		for (final DietPlanDayInput day : dietPlan.days()) {
			final AiToolResult<AiIngestaNutrientCalculator.IngestaNutrientComputation> dayResult = ingestaNutrientCalculator
				.computeIngestas(nutritionistId, day.ingestas());
			if (!dayResult.success()) {
				return AiToolResult.error(Objects.requireNonNull(dayResult.errorCode()),
						Objects.requireNonNull(dayResult.message()));
			}
			final AiIngestaNutrientCalculator.IngestaNutrientComputation dayComputation = Objects
				.requireNonNull(dayResult.data());
			averageDaily = AiNutrientToolSupport.addNutrientSummaries(averageDaily, dayComputation.nutrients());
			warnings.addAll(dayComputation.warnings());
			alimentoIds.addAll(dayComputation.alimentoIds());
		}
		final NutrientSummary dailyAverage = AiNutrientToolSupport.divideNutrientSummary(averageDaily,
				dietPlan.days().size());
		return AiToolResult.success(new ComputationBundle(dailyAverage, warnings, alimentoIds));
	}

	private static AiToolResult<ComputationBundle> mapComputation(
			final AiToolResult<AiIngestaNutrientCalculator.IngestaNutrientComputation> result) {
		if (!result.success()) {
			return AiToolResult.error(Objects.requireNonNull(result.errorCode()),
					Objects.requireNonNull(result.message()));
		}
		final AiIngestaNutrientCalculator.IngestaNutrientComputation computation = Objects
			.requireNonNull(result.data());
		return AiToolResult
			.success(new ComputationBundle(computation.nutrients(), computation.warnings(), computation.alimentoIds()));
	}

	private static AiToolResult<Void> validateRequestBounds(final ValidatePlanConstraintsRequest request) {
		if (request.targetKcal() != null
				&& (request.targetKcal() < MIN_TARGET_KCAL || request.targetKcal() > MAX_TARGET_KCAL)) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, "El objetivo calórico debe estar entre "
					+ (int) MIN_TARGET_KCAL + " y " + (int) MAX_TARGET_KCAL + ".");
		}
		if (request.toleranceKcal() != null && request.toleranceKcal() < 0) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, "La tolerancia calórica no puede ser negativa.");
		}
		return AiToolResult.success(null);
	}

	private record ComputationBundle(NutrientSummary nutrients, List<PlanConstraintWarning> computationWarnings,
			Set<Long> alimentoIds) {
	}

}
