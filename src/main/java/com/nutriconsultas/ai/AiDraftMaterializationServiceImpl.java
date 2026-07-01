package com.nutriconsultas.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.alimentos.AlimentosRepository;
import com.nutriconsultas.dieta.Dieta;
import com.nutriconsultas.dieta.DietaAuthorization;
import com.nutriconsultas.dieta.DietaNutritionCalculator;
import com.nutriconsultas.dieta.DietaService;
import com.nutriconsultas.dieta.Ingesta;
import com.nutriconsultas.platillos.Ingrediente;
import com.nutriconsultas.platillos.Platillo;
import com.nutriconsultas.platillos.PlatilloAuthorization;
import com.nutriconsultas.platillos.PlatilloService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AiDraftMaterializationServiceImpl implements AiDraftMaterializationService {

	static final String DEFAULT_MENU_NAME = "Menú del día";

	static final String DEFAULT_PLAN_NAME = "Plan alimenticio";

	private final PlatilloService platilloService;

	private final PlatilloAuthorization platilloAuthorization;

	private final DietaService dietaService;

	private final DietaAuthorization dietaAuthorization;

	private final AlimentosRepository alimentosRepository;

	private final AiDraftIngestaMaterializer ingestaMaterializer;

	public AiDraftMaterializationServiceImpl(final PlatilloService platilloService,
			final PlatilloAuthorization platilloAuthorization, final DietaService dietaService,
			final DietaAuthorization dietaAuthorization, final AlimentosRepository alimentosRepository,
			final AiDraftIngestaMaterializer ingestaMaterializer) {
		this.platilloService = platilloService;
		this.platilloAuthorization = platilloAuthorization;
		this.dietaService = dietaService;
		this.dietaAuthorization = dietaAuthorization;
		this.alimentosRepository = alimentosRepository;
		this.ingestaMaterializer = ingestaMaterializer;
	}

	@Override
	@Transactional
	public Platillo materializeDish(@NonNull final DishDraftPayload payload, @NonNull final String nutritionistId,
			@NonNull final OidcUser principal) {
		final Platillo platillo = new Platillo();
		platillo.setName(payload.name().trim());
		platillo.setDescription(buildDishDescription(payload));
		platillo.setIngestasSugeridas(payload.ingestasSugeridas());
		platillo.setUserId(platilloAuthorization.resolveCreateUserId(principal, nutritionistId));
		final Platillo saved = platilloService.save(platillo);
		for (final RecipeIngredientInput ingredient : payload.ingredients()) {
			addDishIngredient(saved.getId(), ingredient);
		}
		final Platillo refreshed = platilloService.findById(saved.getId());
		platilloAuthorization.auditSystemPlatilloMutationIfNeeded(principal, refreshed, "ai.drafts.accept.dish");
		if (log.isInfoEnabled()) {
			log.info("AI draft materialized dish platilloId={}", refreshed.getId());
		}
		return refreshed;
	}

	@Override
	@Transactional
	public Dieta materializeMenu(@NonNull final MenuDraftPayload payload, @NonNull final String nutritionistId,
			@NonNull final OidcUser principal) {
		final Dieta dieta = newDieta(resolveMenuName(payload), nutritionistId, principal);
		dieta.setIngestas(buildIngestasFromSlots(payload.ingestas(), nutritionistId, 1, 1));
		return saveCatalogDieta(dieta, principal, "ai.drafts.accept.menu");
	}

	@Override
	@Transactional
	public Dieta materializeDietPlan(@NonNull final DietPlanDraftPayload payload, @NonNull final String nutritionistId,
			@NonNull final OidcUser principal) {
		final Dieta dieta = newDieta(resolvePlanName(payload), nutritionistId, principal);
		final List<Ingesta> ingestas = new ArrayList<>();
		int orden = 1;
		final List<DietPlanDayPayload> sortedDays = payload.days()
			.stream()
			.sorted(Comparator.comparingInt(DietPlanDayPayload::dayIndex))
			.toList();
		for (final DietPlanDayPayload day : sortedDays) {
			orden = appendDayIngestas(ingestas, day, nutritionistId, payload.days().size(), orden);
		}
		dieta.setIngestas(ingestas);
		return saveCatalogDieta(dieta, principal, "ai.drafts.accept.diet_plan");
	}

	private Dieta saveCatalogDieta(final Dieta dieta, final OidcUser principal, final String auditAction) {
		for (final Ingesta ingesta : dieta.getIngestas()) {
			ingesta.setDieta(dieta);
		}
		DietaNutritionCalculator.applyCalculatedNutrients(dieta);
		final Dieta saved = dietaService.saveDieta(dieta);
		dietaAuthorization.auditSystemDietMutationIfNeeded(principal, saved, auditAction);
		if (log.isInfoEnabled()) {
			log.info("AI draft materialized dieta dietaId={}", saved.getId());
		}
		return saved;
	}

	private List<Ingesta> buildIngestasFromSlots(final List<IngestaSlotInput> slots, final String nutritionistId,
			final int dayIndex, final int totalDays) {
		final List<Ingesta> ingestas = new ArrayList<>();
		int orden = 1;
		for (final IngestaSlotInput slot : slots) {
			final Ingesta ingesta = buildIngestaFromSlot(slot, nutritionistId, dayIndex, totalDays, orden);
			orden++;
			ingestas.add(ingesta);
		}
		return ingestas;
	}

	private int appendDayIngestas(final List<Ingesta> ingestas, final DietPlanDayPayload day,
			final String nutritionistId, final int totalDays, final int startOrden) {
		int orden = startOrden;
		for (final IngestaSlotInput slot : day.ingestas()) {
			ingestas.add(buildIngestaFromSlot(slot, nutritionistId, day.dayIndex(), totalDays, orden));
			orden++;
		}
		return orden;
	}

	private Ingesta buildIngestaFromSlot(final IngestaSlotInput slot, final String nutritionistId, final int dayIndex,
			final int totalDays, final int orden) {
		final String nombre = resolveIngestaName(slot, dayIndex, totalDays);
		final Ingesta ingesta = AiDraftIngestaMaterializer.buildIngesta(nombre, orden);
		if (slot.items() != null && !slot.items().isEmpty()) {
			ingestaMaterializer.addItemsToIngesta(ingesta, slot.items(), nutritionistId);
		}
		return ingesta;
	}

	private void addDishIngredient(final Long platilloId, final RecipeIngredientInput ingredient) {
		final Alimento alimento = alimentosRepository.findById(ingredient.alimentoId()).orElse(null);
		if (alimento == null) {
			throw new AiDraftLifecycleException("No se encontró el alimento solicitado.");
		}
		final Integer pesoNeto = ingredient.pesoNetoG() != null ? ingredient.pesoNetoG() : alimento.getPesoNeto();
		if (pesoNeto == null) {
			throw new AiDraftLifecycleException(
					"El alimento " + alimento.getId() + " no tiene peso neto en el catálogo.");
		}
		final Ingrediente added = platilloService.addIngrediente(platilloId, ingredient.alimentoId(),
				ingredient.cantidad().trim(), pesoNeto);
		if (added == null) {
			throw new AiDraftLifecycleException("No se pudo agregar el ingrediente al platillo.");
		}
	}

	private Dieta newDieta(final String nombre, final String nutritionistId, final OidcUser principal) {
		final Dieta dieta = new Dieta();
		dieta.setNombre(nombre);
		dieta.setUserId(dietaAuthorization.resolveCreateUserId(principal, nutritionistId));
		dieta.setPacienteId(null);
		dieta.setIngestas(new ArrayList<>());
		return dieta;
	}

	private static String buildDishDescription(final DishDraftPayload payload) {
		if (!StringUtils.hasText(payload.description())
				&& (payload.preparationSteps() == null || payload.preparationSteps().isEmpty())) {
			return payload.description();
		}
		final StringBuilder builder = new StringBuilder();
		if (StringUtils.hasText(payload.description())) {
			builder.append(payload.description().trim());
		}
		if (payload.preparationSteps() != null && !payload.preparationSteps().isEmpty()) {
			if (!builder.isEmpty()) {
				builder.append("\n\n");
			}
			builder.append("Preparación:\n");
			int step = 1;
			for (final String preparationStep : payload.preparationSteps()) {
				if (StringUtils.hasText(preparationStep)) {
					builder.append(step).append(". ").append(preparationStep.trim()).append('\n');
					step++;
				}
			}
		}
		return builder.isEmpty() ? null : builder.toString().trim();
	}

	private static String resolveMenuName(final MenuDraftPayload payload) {
		if (StringUtils.hasText(payload.title())) {
			return payload.title().trim();
		}
		return DEFAULT_MENU_NAME;
	}

	private static String resolvePlanName(final DietPlanDraftPayload payload) {
		if (StringUtils.hasText(payload.title())) {
			return payload.title().trim();
		}
		return DEFAULT_PLAN_NAME;
	}

	private static String resolveIngestaName(final IngestaSlotInput slot, final int dayIndex, final int totalDays) {
		final String baseName = StringUtils.hasText(slot.nombre()) ? slot.nombre().trim() : "Ingesta";
		if (totalDays <= 1) {
			return baseName;
		}
		return "Día " + dayIndex + " — " + baseName;
	}

}
