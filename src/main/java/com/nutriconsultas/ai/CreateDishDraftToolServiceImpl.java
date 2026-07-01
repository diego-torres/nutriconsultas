package com.nutriconsultas.ai;

import java.util.List;
import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CreateDishDraftToolServiceImpl implements CreateDishDraftToolService {

	static final String DRAFT_LABEL = "Borrador IA — revisión del nutriólogo requerida";

	static final int MIN_NAME_LENGTH = 2;

	static final int MAX_NAME_LENGTH = 120;

	static final int MAX_DESCRIPTION_LENGTH = 2000;

	static final int MAX_INGESTAS_LENGTH = 80;

	static final int MAX_PREPARATION_STEPS = 30;

	static final int MAX_STEP_LENGTH = 500;

	static final int MAX_NOTE_LENGTH = 300;

	static final int MIN_INGREDIENTS = 1;

	static final int MAX_INGREDIENTS = 40;

	private final AiDraftLifecycleService draftLifecycleService;

	private final CalculateRecipeNutrientsToolService recipeNutrientsToolService;

	public CreateDishDraftToolServiceImpl(final AiDraftLifecycleService draftLifecycleService,
			final CalculateRecipeNutrientsToolService recipeNutrientsToolService) {
		this.draftLifecycleService = draftLifecycleService;
		this.recipeNutrientsToolService = recipeNutrientsToolService;
	}

	@Override
	@Transactional
	public AiToolResult<AiDraftCreationData> createDraft(@NonNull final String nutritionistId, final long threadId,
			@NonNull final DishDraftInput input) {
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

		final int portions = input.portions() == null ? 1 : input.portions();
		final AiToolResult<RecipeNutrientsData> nutrientsResult = recipeNutrientsToolService.calculate(nutritionistId,
				input.ingredients(), portions, null);
		if (!nutrientsResult.success()) {
			return AiToolResult.error(Objects.requireNonNull(nutrientsResult.errorCode()),
					Objects.requireNonNull(nutrientsResult.message()));
		}
		final RecipeNutrientsData nutrientsData = Objects.requireNonNull(nutrientsResult.data());
		final DishDraftPayload payload = new DishDraftPayload(input.name().trim(), trimToNull(input.description()),
				input.preparationSteps(), trimToNull(input.ingestasSugeridas()), input.ingredients(), portions,
				nutrientsData.nutrientsPerPortion(), input.assumptions(), input.warnings(), DRAFT_LABEL);
		try {
			final String jsonPayload = AiDraftPayloadSerializer.toJson(payload);
			final AiGeneratedDraft draft = draftLifecycleService.createDraft(threadId, nutritionistId, AiDraftType.DISH,
					jsonPayload);
			final String summary = DRAFT_LABEL + ": " + input.name().trim();
			final AiDraftCreationData data = new AiDraftCreationData(draft.getId(), AiDraftType.DISH, draft.getStatus(),
					summary);
			if (log.isInfoEnabled()) {
				log.info("AI tool create_dish_draft draftId={} threadId={}", draft.getId(), threadId);
			}
			return AiToolResult.success(data);
		}
		catch (AiDraftLifecycleException ex) {
			return mapLifecycleException(ex);
		}
	}

	private static AiToolResult<Void> validateInput(final DishDraftInput input) {
		if (!StringUtils.hasText(input.name()) || input.name().trim().length() < MIN_NAME_LENGTH) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION,
					"El nombre del platillo debe tener al menos " + MIN_NAME_LENGTH + " caracteres.");
		}
		if (input.name().trim().length() > MAX_NAME_LENGTH) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION,
					"El nombre del platillo no puede superar " + MAX_NAME_LENGTH + " caracteres.");
		}
		if (input.description() != null && input.description().length() > MAX_DESCRIPTION_LENGTH) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION,
					"La descripción no puede superar " + MAX_DESCRIPTION_LENGTH + " caracteres.");
		}
		if (input.ingestasSugeridas() != null && input.ingestasSugeridas().length() > MAX_INGESTAS_LENGTH) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION,
					"Las ingestas sugeridas no pueden superar " + MAX_INGESTAS_LENGTH + " caracteres.");
		}
		if (input.ingredients() == null || input.ingredients().size() < MIN_INGREDIENTS
				|| input.ingredients().size() > MAX_INGREDIENTS) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION,
					"El platillo debe tener entre " + MIN_INGREDIENTS + " y " + MAX_INGREDIENTS + " ingredientes.");
		}
		if (input.portions() != null && input.portions() < 1) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, "Las porciones deben ser al menos 1.");
		}
		final AiToolResult<Void> stepsValidation = validateStringList(input.preparationSteps(), MAX_PREPARATION_STEPS,
				MAX_STEP_LENGTH, "paso de preparación");
		if (!stepsValidation.success()) {
			return stepsValidation;
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

	private static AiToolResult<Void> validateStringList(final List<String> values, final int maxItems,
			final int maxLength, final String label) {
		if (values == null) {
			return AiToolResult.success(null);
		}
		if (values.size() > maxItems) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION,
					"El platillo no puede tener más de " + maxItems + " " + label + "s.");
		}
		for (final String value : values) {
			if (value != null && value.length() > maxLength) {
				return AiToolResult.error(AiToolErrorCode.VALIDATION,
						"Cada " + label + " no puede superar " + maxLength + " caracteres.");
			}
		}
		return AiToolResult.success(null);
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

}
