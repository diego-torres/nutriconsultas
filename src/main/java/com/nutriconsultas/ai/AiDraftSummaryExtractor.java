package com.nutriconsultas.ai;

import org.springframework.util.StringUtils;

/**
 * Human-readable draft summaries for chat REST responses (#384).
 */
public final class AiDraftSummaryExtractor {

	public static final String REVIEW_LABEL = "Borrador IA — revisión del nutriólogo requerida";

	private static final String DEFAULT_LABEL = REVIEW_LABEL;

	private AiDraftSummaryExtractor() {
	}

	public static String summarize(final AiGeneratedDraft draft) {
		if (draft == null || draft.getDraftType() == null) {
			return DEFAULT_LABEL;
		}
		try {
			return switch (draft.getDraftType()) {
				case DISH -> summarizeDish(draft);
				case MENU -> summarizeMenu(draft);
				case DIET_PLAN -> summarizeDietPlan(draft);
			};
		}
		catch (final AiDraftLifecycleException ex) {
			return DEFAULT_LABEL;
		}
	}

	private static String summarizeDish(final AiGeneratedDraft draft) {
		final DishDraftPayload payload = AiDraftPayloadDeserializer.dish(draft.getJsonPayload());
		if (StringUtils.hasText(payload.name())) {
			return DEFAULT_LABEL + ": " + payload.name().trim();
		}
		return DEFAULT_LABEL;
	}

	private static String summarizeMenu(final AiGeneratedDraft draft) {
		final MenuDraftPayload payload = AiDraftPayloadDeserializer.menu(draft.getJsonPayload());
		if (StringUtils.hasText(payload.title())) {
			return DEFAULT_LABEL + ": " + payload.title().trim();
		}
		return DEFAULT_LABEL;
	}

	private static String summarizeDietPlan(final AiGeneratedDraft draft) {
		final DietPlanDraftPayload payload = AiDraftPayloadDeserializer.dietPlan(draft.getJsonPayload());
		if (StringUtils.hasText(payload.title())) {
			return DEFAULT_LABEL + ": " + payload.title().trim();
		}
		return DEFAULT_LABEL;
	}

}
