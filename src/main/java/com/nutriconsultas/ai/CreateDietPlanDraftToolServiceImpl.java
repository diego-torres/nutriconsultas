package com.nutriconsultas.ai;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CreateDietPlanDraftToolServiceImpl implements CreateDietPlanDraftToolService {

	static final String DRAFT_LABEL = "Borrador IA — revisión del nutriólogo requerida";

	static final String DEFAULT_PLAN_TITLE = "Plan alimenticio";

	static final int MAX_TITLE_LENGTH = 120;

	static final int MIN_DAYS = 1;

	static final int MAX_DAYS = 14;

	static final int MAX_DAY_LABEL_LENGTH = 80;

	static final int MIN_INGESTAS = 1;

	static final int MAX_INGESTAS = 12;

	static final int MAX_INGESTA_NAME_LENGTH = 80;

	static final int MAX_VALIDATION_SUMMARY_LENGTH = 2000;

	static final int MAX_NOTE_LENGTH = 300;

	private final AiDraftLifecycleService draftLifecycleService;

	private final AiIngestaNutrientCalculator ingestaNutrientCalculator;

	public CreateDietPlanDraftToolServiceImpl(final AiDraftLifecycleService draftLifecycleService,
			final AiIngestaNutrientCalculator ingestaNutrientCalculator) {
		this.draftLifecycleService = draftLifecycleService;
		this.ingestaNutrientCalculator = ingestaNutrientCalculator;
	}

	@Override
	@Transactional
	public AiToolResult<AiDraftCreationData> createDraft(@NonNull final String nutritionistId, final long threadId,
			@NonNull final DietPlanDraftInput input) {
		if (!StringUtils.hasText(nutritionistId)) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, "Sesión de nutriólogo no válida.");
		}
		if (threadId <= 0) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, "La conversación no es válida.");
		}
		final AiToolResult<Void> validation = validateInput(input);
		if (!validation.success()) {
			return AiToolResult.error(Objects.requireNonNull(validation.errorCode()),
					Objects.requireNonNull(validation.message()));
		}

		final AiToolResult<PlanNutrientComputation> nutrientsResult = computePlanNutrients(nutritionistId,
				input.days());
		if (!nutrientsResult.success()) {
			return AiToolResult.error(Objects.requireNonNull(nutrientsResult.errorCode()),
					Objects.requireNonNull(nutrientsResult.message()));
		}
		final PlanNutrientComputation computation = Objects.requireNonNull(nutrientsResult.data());
		final String displayTitle = resolveDisplayTitle(input);
		final DietPlanDraftPayload payload = new DietPlanDraftPayload(trimToNull(input.title()), input.days().size(),
				input.targetKcalPerDay(), computation.dayPayloads(), computation.weeklyAverageNutrients(),
				trimToNull(input.validationSummary()), input.assumptions(), input.warnings(), DRAFT_LABEL);
		try {
			final String jsonPayload = AiDraftPayloadSerializer.toJson(payload);
			final AiGeneratedDraft draft = draftLifecycleService.createDraft(threadId, nutritionistId,
					AiDraftType.DIET_PLAN, jsonPayload);
			final String summary = DRAFT_LABEL + ": " + displayTitle;
			final AiDraftCreationData data = new AiDraftCreationData(draft.getId(), AiDraftType.DIET_PLAN,
					draft.getStatus(), summary);
			if (log.isInfoEnabled()) {
				log.info("AI tool create_diet_plan_draft draftId={} threadId={} dayCount={}", draft.getId(), threadId,
						input.days().size());
			}
			return AiToolResult.success(data);
		}
		catch (AiDraftLifecycleException ex) {
			return mapLifecycleException(ex);
		}
	}

	private AiToolResult<PlanNutrientComputation> computePlanNutrients(final String nutritionistId,
			final List<DietPlanDayInput> days) {
		final List<DietPlanDayPayload> dayPayloads = new ArrayList<>();
		NutrientSummary weeklySum = AiNutrientToolSupport.emptyNutrientSummary();
		for (final DietPlanDayInput day : days) {
			final AiToolResult<AiIngestaNutrientCalculator.IngestaNutrientComputation> dayResult = ingestaNutrientCalculator
				.computeIngestas(nutritionistId, day.ingestas());
			if (!dayResult.success()) {
				return AiToolResult.error(Objects.requireNonNull(dayResult.errorCode()),
						Objects.requireNonNull(dayResult.message()));
			}
			final NutrientSummary dayNutrients = Objects.requireNonNull(dayResult.data()).nutrients();
			dayPayloads
				.add(new DietPlanDayPayload(day.dayIndex(), trimToNull(day.label()), day.ingestas(), dayNutrients));
			weeklySum = AiNutrientToolSupport.addNutrientSummaries(weeklySum, dayNutrients);
		}
		final NutrientSummary weeklyAverage = AiNutrientToolSupport.divideNutrientSummary(weeklySum, days.size());
		return AiToolResult.success(new PlanNutrientComputation(dayPayloads, weeklyAverage));
	}

	private static AiToolResult<Void> validateInput(final DietPlanDraftInput input) {
		if (input.title() != null && input.title().length() > MAX_TITLE_LENGTH) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION,
					"El título del plan no puede superar " + MAX_TITLE_LENGTH + " caracteres.");
		}
		if (input.targetKcalPerDay() != null && input.targetKcalPerDay() < 0) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, "El objetivo calórico diario no puede ser negativo.");
		}
		if (input.days() == null || input.days().size() < MIN_DAYS || input.days().size() > MAX_DAYS) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION,
					"El plan debe tener entre " + MIN_DAYS + " y " + MAX_DAYS + " días.");
		}
		if (input.dayCount() != null && (input.dayCount() < MIN_DAYS || input.dayCount() > MAX_DAYS
				|| input.dayCount() != input.days().size())) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION,
					"El número de días no coincide con los días del plan.");
		}
		final Set<Integer> dayIndexes = new HashSet<>();
		for (final DietPlanDayInput day : input.days()) {
			final AiToolResult<Void> dayValidation = validateDay(day, dayIndexes);
			if (!dayValidation.success()) {
				return dayValidation;
			}
		}
		if (input.validationSummary() != null && input.validationSummary().length() > MAX_VALIDATION_SUMMARY_LENGTH) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION,
					"El resumen de validación no puede superar " + MAX_VALIDATION_SUMMARY_LENGTH + " caracteres.");
		}
		final AiToolResult<Void> assumptionsValidation = validateStringList(input.assumptions(), Integer.MAX_VALUE,
				MAX_NOTE_LENGTH, "supuesto");
		if (!assumptionsValidation.success()) {
			return assumptionsValidation;
		}
		final AiToolResult<Void> warningsValidation = validateStringList(input.warnings(), Integer.MAX_VALUE,
				MAX_NOTE_LENGTH, "advertencia");
		if (!warningsValidation.success()) {
			return warningsValidation;
		}
		return AiToolResult.success(null);
	}

	private static AiToolResult<Void> validateDay(final DietPlanDayInput day, final Set<Integer> dayIndexes) {
		if (day.dayIndex() < 1) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, "El índice del día debe ser al menos 1.");
		}
		if (!dayIndexes.add(day.dayIndex())) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, "El plan no puede repetir el mismo día.");
		}
		if (day.label() != null && day.label().length() > MAX_DAY_LABEL_LENGTH) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION,
					"La etiqueta del día no puede superar " + MAX_DAY_LABEL_LENGTH + " caracteres.");
		}
		if (day.ingestas() == null || day.ingestas().size() < MIN_INGESTAS || day.ingestas().size() > MAX_INGESTAS) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION,
					"Cada día debe tener entre " + MIN_INGESTAS + " y " + MAX_INGESTAS + " ingestas.");
		}
		for (final IngestaSlotInput ingesta : day.ingestas()) {
			final AiToolResult<Void> ingestaValidation = validateIngesta(ingesta);
			if (!ingestaValidation.success()) {
				return ingestaValidation;
			}
		}
		return AiToolResult.success(null);
	}

	private static AiToolResult<Void> validateIngesta(final IngestaSlotInput ingesta) {
		if (ingesta.nombre() != null && ingesta.nombre().length() > MAX_INGESTA_NAME_LENGTH) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION,
					"El nombre de la ingesta no puede superar " + MAX_INGESTA_NAME_LENGTH + " caracteres.");
		}
		if (ingesta.items() == null || ingesta.items().isEmpty()) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, "Cada ingesta debe incluir al menos un ítem.");
		}
		for (final IngestaSlotItemInput item : ingesta.items()) {
			if (item.portions() != null && item.portions() < 1) {
				return AiToolResult.error(AiToolErrorCode.VALIDATION, "Las porciones deben ser al menos 1.");
			}
		}
		return AiToolResult.success(null);
	}

	private static AiToolResult<Void> validateStringList(final List<String> values, final int maxItems,
			final int maxLength, final String label) {
		if (values == null) {
			return AiToolResult.success(null);
		}
		if (values.size() > maxItems) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION,
					"El plan no puede tener más de " + maxItems + " " + label + "s.");
		}
		for (final String value : values) {
			if (value != null && value.length() > maxLength) {
				return AiToolResult.error(AiToolErrorCode.VALIDATION,
						"Cada " + label + " no puede superar " + maxLength + " caracteres.");
			}
		}
		return AiToolResult.success(null);
	}

	private static String resolveDisplayTitle(final DietPlanDraftInput input) {
		if (StringUtils.hasText(input.title())) {
			return input.title().trim();
		}
		return DEFAULT_PLAN_TITLE;
	}

	private static AiToolResult<AiDraftCreationData> mapLifecycleException(final AiDraftLifecycleException ex) {
		if (ex.getMessage() != null && ex.getMessage().contains("Conversación no encontrada")) {
			return AiToolResult.error(AiToolErrorCode.NOT_FOUND, ex.getMessage());
		}
		return AiToolResult.error(AiToolErrorCode.VALIDATION, ex.getMessage());
	}

	private static String trimToNull(final String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}

	private record PlanNutrientComputation(List<DietPlanDayPayload> dayPayloads,
			NutrientSummary weeklyAverageNutrients) {
	}

}
