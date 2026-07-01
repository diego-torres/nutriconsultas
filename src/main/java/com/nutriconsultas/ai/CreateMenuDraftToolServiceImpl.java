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
public class CreateMenuDraftToolServiceImpl implements CreateMenuDraftToolService {

	static final String DRAFT_LABEL = "Borrador IA — revisión del nutriólogo requerida";

	static final String DEFAULT_MENU_TITLE = "Menú del día";

	static final int MAX_TITLE_LENGTH = 120;

	static final int MIN_INGESTAS = 1;

	static final int MAX_INGESTAS = 12;

	static final int MAX_INGESTA_NAME_LENGTH = 80;

	static final int MAX_VALIDATION_SUMMARY_LENGTH = 1000;

	static final int MAX_NOTE_LENGTH = 300;

	private final AiDraftLifecycleService draftLifecycleService;

	private final AiIngestaNutrientCalculator ingestaNutrientCalculator;

	public CreateMenuDraftToolServiceImpl(final AiDraftLifecycleService draftLifecycleService,
			final AiIngestaNutrientCalculator ingestaNutrientCalculator) {
		this.draftLifecycleService = draftLifecycleService;
		this.ingestaNutrientCalculator = ingestaNutrientCalculator;
	}

	@Override
	@Transactional
	public AiToolResult<AiDraftCreationData> createDraft(@NonNull final String nutritionistId, final long threadId,
			@NonNull final MenuDraftInput input) {
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

		final AiToolResult<AiIngestaNutrientCalculator.IngestaNutrientComputation> nutrientsResult = ingestaNutrientCalculator
			.computeIngestas(nutritionistId, input.ingestas());
		if (!nutrientsResult.success()) {
			return AiToolResult.error(Objects.requireNonNull(nutrientsResult.errorCode()),
					Objects.requireNonNull(nutrientsResult.message()));
		}
		final NutrientSummary nutrientsTotal = Objects.requireNonNull(nutrientsResult.data()).nutrients();
		final String displayTitle = resolveDisplayTitle(input);
		final MenuDraftPayload payload = new MenuDraftPayload(trimToNull(input.title()), input.targetKcal(),
				input.ingestas(), nutrientsTotal, trimToNull(input.validationSummary()), input.assumptions(),
				input.warnings(), DRAFT_LABEL);
		try {
			final String jsonPayload = AiDraftPayloadSerializer.toJson(payload);
			final AiGeneratedDraft draft = draftLifecycleService.createDraft(threadId, nutritionistId, AiDraftType.MENU,
					jsonPayload);
			final String summary = DRAFT_LABEL + ": " + displayTitle;
			final AiDraftCreationData data = new AiDraftCreationData(draft.getId(), AiDraftType.MENU, draft.getStatus(),
					summary);
			if (log.isInfoEnabled()) {
				log.info("AI tool create_menu_draft draftId={} threadId={}", draft.getId(), threadId);
			}
			return AiToolResult.success(data);
		}
		catch (AiDraftLifecycleException ex) {
			return mapLifecycleException(ex);
		}
	}

	private static AiToolResult<Void> validateInput(final MenuDraftInput input) {
		if (input.title() != null && input.title().length() > MAX_TITLE_LENGTH) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION,
					"El título del menú no puede superar " + MAX_TITLE_LENGTH + " caracteres.");
		}
		if (input.targetKcal() != null && input.targetKcal() < 0) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, "El objetivo calórico no puede ser negativo.");
		}
		if (input.ingestas() == null || input.ingestas().size() < MIN_INGESTAS
				|| input.ingestas().size() > MAX_INGESTAS) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION,
					"El menú debe tener entre " + MIN_INGESTAS + " y " + MAX_INGESTAS + " ingestas.");
		}
		for (final IngestaSlotInput ingesta : input.ingestas()) {
			final AiToolResult<Void> ingestaValidation = validateIngesta(ingesta);
			if (!ingestaValidation.success()) {
				return ingestaValidation;
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
					"El menú no puede tener más de " + maxItems + " " + label + "s.");
		}
		for (final String value : values) {
			if (value != null && value.length() > maxLength) {
				return AiToolResult.error(AiToolErrorCode.VALIDATION,
						"Cada " + label + " no puede superar " + maxLength + " caracteres.");
			}
		}
		return AiToolResult.success(null);
	}

	private static String resolveDisplayTitle(final MenuDraftInput input) {
		if (StringUtils.hasText(input.title())) {
			return input.title().trim();
		}
		return DEFAULT_MENU_TITLE;
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
