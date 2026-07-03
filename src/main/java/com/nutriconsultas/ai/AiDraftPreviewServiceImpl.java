package com.nutriconsultas.ai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AiDraftPreviewServiceImpl implements AiDraftPreviewService {

	private final AiGeneratedDraftRepository draftRepository;

	public AiDraftPreviewServiceImpl(final AiGeneratedDraftRepository draftRepository) {
		this.draftRepository = draftRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public AiDraftPreviewView getPreview(final long draftId, @NonNull final String nutritionistId) {
		final AiGeneratedDraft draft = draftRepository.findByIdAndThreadNutritionistId(draftId, nutritionistId)
			.orElseThrow(() -> new AiDraftLifecycleException("Borrador no encontrado."));
		return toPreviewView(draft);
	}

	private AiDraftPreviewView toPreviewView(final AiGeneratedDraft draft) {
		final String summary = AiDraftSummaryExtractor.summarize(draft);
		return switch (draft.getDraftType()) {
			case DISH -> fromDish(draft, summary);
			case MENU -> fromMenu(draft, summary);
			case DIET_PLAN -> fromDietPlan(draft, summary);
		};
	}

	private AiDraftPreviewView fromDish(final AiGeneratedDraft draft, final String summary) {
		final DishDraftPayload payload = AiDraftPayloadDeserializer.dish(draft.getJsonPayload());
		final String reviewLabel = StringUtils.hasText(payload.label()) ? payload.label()
				: AiDraftSummaryExtractor.REVIEW_LABEL;
		return new AiDraftPreviewView(draft.getId(), draft.getThread().getId(), draft.getDraftType(), draft.getStatus(),
				"Platillo (receta)", reviewLabel, payload.name(), summary, payload.portions(), null,
				payload.nutrientsPerPortion(), toIngredientLines(payload.ingredients()), List.of(),
				safeList(payload.preparationSteps()), safeList(payload.assumptions()), safeList(payload.warnings()),
				null);
	}

	private AiDraftPreviewView fromMenu(final AiGeneratedDraft draft, final String summary) {
		final MenuDraftPayload payload = AiDraftPayloadDeserializer.menu(draft.getJsonPayload());
		final String reviewLabel = StringUtils.hasText(payload.label()) ? payload.label()
				: AiDraftSummaryExtractor.REVIEW_LABEL;
		return new AiDraftPreviewView(draft.getId(), draft.getThread().getId(), draft.getDraftType(), draft.getStatus(),
				"Menú", reviewLabel, payload.title(), summary, null, null, payload.nutrientsTotal(), List.of(),
				toMealSlots(payload.ingestas()), List.of(), safeList(payload.assumptions()),
				safeList(payload.warnings()), payload.validationSummary());
	}

	private AiDraftPreviewView fromDietPlan(final AiGeneratedDraft draft, final String summary) {
		final DietPlanDraftPayload payload = AiDraftPayloadDeserializer.dietPlan(draft.getJsonPayload());
		final String reviewLabel = StringUtils.hasText(payload.label()) ? payload.label()
				: AiDraftSummaryExtractor.REVIEW_LABEL;
		final List<Map<String, Object>> mealSlots = new ArrayList<>();
		if (payload.days() != null) {
			for (final DietPlanDayPayload day : payload.days()) {
				final Map<String, Object> dayMap = new LinkedHashMap<>();
				dayMap.put("dayIndex", day.dayIndex());
				dayMap.put("label", day.label());
				dayMap.put("ingestas", toMealSlots(day.ingestas()));
				dayMap.put("nutrients", day.nutrientsTotal());
				mealSlots.add(dayMap);
			}
		}
		return new AiDraftPreviewView(draft.getId(), draft.getThread().getId(), draft.getDraftType(), draft.getStatus(),
				"Plan alimentario", reviewLabel, payload.title(), summary, null, payload.dayCount(),
				payload.weeklyAverageNutrients(), List.of(), mealSlots, List.of(), safeList(payload.assumptions()),
				safeList(payload.warnings()), payload.validationSummary());
	}

	private static List<Map<String, String>> toIngredientLines(final List<RecipeIngredientInput> ingredients) {
		final List<Map<String, String>> lines = new ArrayList<>();
		if (ingredients == null) {
			return lines;
		}
		for (final RecipeIngredientInput ingredient : ingredients) {
			final Map<String, String> line = new LinkedHashMap<>();
			line.put("alimentoId", String.valueOf(ingredient.alimentoId()));
			line.put("cantidad", ingredient.cantidad());
			line.put("unidad", ingredient.unidad() != null ? ingredient.unidad() : "");
			if (ingredient.pesoNetoG() != null) {
				line.put("pesoNetoG", String.valueOf(ingredient.pesoNetoG()));
			}
			lines.add(line);
		}
		return lines;
	}

	private static List<Map<String, Object>> toMealSlots(final List<IngestaSlotInput> ingestas) {
		final List<Map<String, Object>> slots = new ArrayList<>();
		if (ingestas == null) {
			return slots;
		}
		for (final IngestaSlotInput ingesta : ingestas) {
			final Map<String, Object> slot = new LinkedHashMap<>();
			slot.put("nombre", ingesta.nombre());
			slot.put("orden", ingesta.orden());
			slot.put("items", toMealItems(ingesta.items()));
			slots.add(slot);
		}
		return slots;
	}

	private static List<Map<String, String>> toMealItems(final List<IngestaSlotItemInput> items) {
		final List<Map<String, String>> result = new ArrayList<>();
		if (items == null) {
			return result;
		}
		for (final IngestaSlotItemInput item : items) {
			final Map<String, String> map = new LinkedHashMap<>();
			map.put("type", item.type());
			if (item.platilloId() != null) {
				map.put("platilloId", String.valueOf(item.platilloId()));
			}
			if (item.alimentoId() != null) {
				map.put("alimentoId", String.valueOf(item.alimentoId()));
			}
			if (item.portions() != null) {
				map.put("portions", String.valueOf(item.portions()));
			}
			result.add(map);
		}
		return result;
	}

	private static List<String> safeList(final List<String> values) {
		return values != null ? List.copyOf(values) : List.of();
	}

}
