package com.nutriconsultas.dieta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.platillos.Platillo;
import com.nutriconsultas.platillos.PlatilloAuthorization;
import com.nutriconsultas.platillos.PlatilloIngredientLimits;
import com.nutriconsultas.platillos.PlatilloService;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class PlatilloFromIngestaServiceImpl implements PlatilloFromIngestaService {

	private final PlatilloService platilloService;

	private final PlatilloAuthorization platilloAuthorization;

	private final DietaService dietaService;

	public PlatilloFromIngestaServiceImpl(final PlatilloService platilloService,
			final PlatilloAuthorization platilloAuthorization, final DietaService dietaService) {
		this.platilloService = platilloService;
		this.platilloAuthorization = platilloAuthorization;
		this.dietaService = dietaService;
	}

	@Override
	public Platillo createFromIngestaSelection(@NonNull final Ingesta ingesta,
			@NonNull final CreatePlatilloFromIngestaRequest request, @NonNull final String userId,
			final OidcUser principal) {
		final String nombre = request.getNombre() != null ? request.getNombre().trim() : "";
		if (!StringUtils.hasText(nombre)) {
			throw new IllegalArgumentException("El nombre del platillo es obligatorio");
		}

		final List<Long> alimentoIds = request.getAlimentoIngestaIds() != null ? request.getAlimentoIngestaIds()
				: List.of();
		final List<Long> platilloIds = request.getPlatilloIngestaIds() != null ? request.getPlatilloIngestaIds()
				: List.of();
		if (alimentoIds.isEmpty() && platilloIds.isEmpty()) {
			throw new IllegalArgumentException("Seleccione al menos un alimento o platillo");
		}
		if (platilloIds.size() == 1 && alimentoIds.isEmpty()) {
			throw new IllegalArgumentException(
					"Este platillo ya existe en el catálogo. Use la opción Copiar platillo en el formulario del platillo.");
		}

		final List<AlimentoIngesta> selectedAlimentos = resolveSelectedAlimentos(ingesta, alimentoIds);
		final List<PlatilloIngesta> selectedPlatillos = resolveSelectedPlatillos(ingesta, platilloIds);
		if (selectedAlimentos.isEmpty() && selectedPlatillos.isEmpty()) {
			throw new IllegalArgumentException("La selección no pertenece a esta ingesta");
		}

		final List<IngredientDraft> drafts = collectIngredientDrafts(selectedAlimentos, selectedPlatillos);
		if (drafts.isEmpty()) {
			throw new IllegalArgumentException("No se encontraron ingredientes en la selección");
		}
		if (drafts.size() > PlatilloIngredientLimits.MAX_PER_PLATILLO) {
			throw new IllegalArgumentException("La selección supera el máximo de "
					+ PlatilloIngredientLimits.MAX_PER_PLATILLO + " ingredientes por platillo");
		}

		final Platillo platillo = new Platillo();
		platillo.setName(nombre);
		platillo.setUserId(platilloAuthorization.resolveCreateUserId(principal, userId));
		if (StringUtils.hasText(ingesta.getNombre())) {
			platillo.setIngestasSugeridas(ingesta.getNombre());
		}
		final Platillo saved = platilloService.save(platillo);
		log.info("Creating platillo {} from ingesta {} with {} ingredient drafts", saved.getId(), ingesta.getId(),
				drafts.size());

		for (final IngredientDraft draft : drafts) {
			final IngredientePlatilloIngesta helper = new IngredientePlatilloIngesta();
			helper.setCantSugerida(draft.cantSugerida());
			helper.setPesoNeto(draft.pesoNeto());
			helper.setPesoBrutoRedondeado(draft.pesoBruto());
			final String cantidad = draft.cantidadForAdd();
			final Integer peso = draft.pesoNeto();
			platilloService.addIngrediente(saved.getId(), draft.alimentoId(), cantidad, peso);
		}

		platilloAuthorization.auditSystemPlatilloMutationIfNeeded(principal, saved,
				"platillos.create-from-dieta-ingesta");
		return platilloService.findById(saved.getId());
	}

	@Override
	public Dieta replaceSelectionWithCatalogPlatillo(@NonNull final Dieta dieta, @NonNull final Ingesta ingesta,
			@NonNull final Long catalogPlatilloId, @NonNull final ReplaceIngestaSelectionRequest request) {
		final List<Long> alimentoIds = request.getAlimentoIngestaIds() != null ? request.getAlimentoIngestaIds()
				: List.of();
		final List<Long> platilloIds = request.getPlatilloIngestaIds() != null ? request.getPlatilloIngestaIds()
				: List.of();
		if (alimentoIds.isEmpty() && platilloIds.isEmpty()) {
			throw new IllegalArgumentException("Seleccione al menos un alimento o platillo para reemplazar");
		}

		final Set<Long> alimentoIdSet = Set.copyOf(alimentoIds);
		final Set<Long> platilloIdSet = Set.copyOf(platilloIds);
		if (ingesta.getAlimentos() != null) {
			final boolean allAlimentosFound = alimentoIdSet.stream()
				.allMatch(id -> ingesta.getAlimentos().stream().anyMatch(row -> id.equals(row.getId())));
			if (!allAlimentosFound) {
				throw new IllegalArgumentException("La selección de alimentos no pertenece a esta ingesta");
			}
			ingesta.getAlimentos().removeIf(row -> alimentoIdSet.contains(row.getId()));
		}
		else if (!alimentoIdSet.isEmpty()) {
			throw new IllegalArgumentException("La selección de alimentos no pertenece a esta ingesta");
		}

		if (ingesta.getPlatillos() != null) {
			final boolean allPlatillosFound = platilloIdSet.stream()
				.allMatch(id -> ingesta.getPlatillos().stream().anyMatch(row -> id.equals(row.getId())));
			if (!allPlatillosFound) {
				throw new IllegalArgumentException("La selección de platillos no pertenece a esta ingesta");
			}
			ingesta.getPlatillos().removeIf(row -> platilloIdSet.contains(row.getId()));
		}
		else if (!platilloIdSet.isEmpty()) {
			throw new IllegalArgumentException("La selección de platillos no pertenece a esta ingesta");
		}

		final Platillo catalogPlatillo = platilloService.findById(catalogPlatilloId);
		if (catalogPlatillo == null) {
			throw new IllegalArgumentException("Platillo de catálogo no encontrado");
		}
		PlatilloIngestaMapping.attachCatalogPlatilloToIngesta(catalogPlatillo, ingesta, 1);
		log.info("Replaced ingesta {} selection with catalog platillo {}", ingesta.getId(), catalogPlatilloId);
		return dietaService.saveDieta(dieta);
	}

	private List<AlimentoIngesta> resolveSelectedAlimentos(final Ingesta ingesta, final List<Long> requestedIds) {
		if (requestedIds.isEmpty() || ingesta.getAlimentos() == null) {
			return List.of();
		}
		final Set<Long> idSet = Set.copyOf(requestedIds);
		return ingesta.getAlimentos()
			.stream()
			.filter(alimento -> idSet.contains(alimento.getId()))
			.sorted(AlimentoIngestaComparators.BY_DISPLAY_ORDER)
			.toList();
	}

	private List<PlatilloIngesta> resolveSelectedPlatillos(final Ingesta ingesta, final List<Long> requestedIds) {
		if (requestedIds.isEmpty() || ingesta.getPlatillos() == null) {
			return List.of();
		}
		final Map<Long, PlatilloIngesta> byId = ingesta.getPlatillos()
			.stream()
			.collect(Collectors.toMap(PlatilloIngesta::getId, platillo -> platillo, (left, right) -> left,
					LinkedHashMap::new));
		final List<PlatilloIngesta> selected = new ArrayList<>();
		for (final Long id : requestedIds) {
			final PlatilloIngesta platillo = byId.get(id);
			if (platillo != null) {
				selected.add(platillo);
			}
		}
		return selected;
	}

	private List<IngredientDraft> collectIngredientDrafts(final List<AlimentoIngesta> alimentos,
			final List<PlatilloIngesta> platillos) {
		final Map<Long, IngredientDraft> merged = new LinkedHashMap<>();
		int order = 0;
		for (final AlimentoIngesta alimentoIngesta : alimentos) {
			final int draftOrder = order;
			order++;
			final IngredientDraft draft = draftFromAlimentoIngesta(alimentoIngesta, draftOrder);
			mergeDraft(merged, draft);
		}
		for (final PlatilloIngesta platilloIngesta : platillos) {
			final int platilloPortions = platilloIngesta.getPortions() != null ? platilloIngesta.getPortions() : 1;
			if (platilloIngesta.getIngredientes() == null) {
				continue;
			}
			for (final IngredientePlatilloIngesta ingrediente : platilloIngesta.getIngredientes()) {
				final int draftOrder = order;
				order++;
				final IngredientDraft draft = draftFromPlatilloIngrediente(ingrediente, platilloPortions, draftOrder);
				if (draft != null) {
					mergeDraft(merged, draft);
				}
			}
		}
		return List.copyOf(merged.values());
	}

	private void mergeDraft(final Map<Long, IngredientDraft> merged, final IngredientDraft draft) {
		final IngredientDraft existing = merged.get(draft.alimentoId());
		if (existing == null) {
			merged.put(draft.alimentoId(), draft);
			return;
		}
		merged.put(draft.alimentoId(), existing.mergeWith(draft));
	}

	private IngredientDraft draftFromAlimentoIngesta(final AlimentoIngesta alimentoIngesta, final int order) {
		final Alimento alimento = alimentoIngesta.getAlimento();
		if (alimento == null || alimento.getId() == null) {
			throw new IllegalArgumentException("Alimento de ingesta sin catálogo asociado");
		}
		final int portions = alimentoIngesta.getPortions() != null ? alimentoIngesta.getPortions() : 1;
		final double catalogCant = alimento.getCantSugerida() != null ? alimento.getCantSugerida() : 1.0;
		final double cantSugerida = catalogCant * portions;
		final int pesoNeto = alimentoIngesta.getPesoNeto() != null ? alimentoIngesta.getPesoNeto()
				: Objects.requireNonNullElse(alimento.getPesoNeto(), 0);
		final int pesoBruto = alimentoIngesta.getPesoBrutoRedondeado() != null
				? alimentoIngesta.getPesoBrutoRedondeado()
				: Objects.requireNonNullElse(alimento.getPesoBrutoRedondeado(), pesoNeto);
		final String unidad = alimentoIngesta.getUnidad() != null ? alimentoIngesta.getUnidad() : alimento.getUnidad();
		return new IngredientDraft(alimento.getId(), cantSugerida, pesoNeto, pesoBruto, unidad, order);
	}

	private IngredientDraft draftFromPlatilloIngrediente(final IngredientePlatilloIngesta ingrediente,
			final int platilloPortions, final int order) {
		final Alimento alimento = ingrediente.getAlimento();
		if (alimento == null || alimento.getId() == null) {
			return null;
		}
		final double baseCant = ingrediente.getCantSugerida() != null ? ingrediente.getCantSugerida() : 1.0;
		final double cantSugerida = baseCant * platilloPortions;
		final int pesoNeto = scaleInteger(ingrediente.getPesoNeto(), platilloPortions);
		final int pesoBruto = scaleInteger(ingrediente.getPesoBrutoRedondeado(), platilloPortions);
		return new IngredientDraft(alimento.getId(), cantSugerida, pesoNeto, pesoBruto, ingrediente.getUnidad(), order);
	}

	private int scaleInteger(final Integer value, final int factor) {
		if (value == null) {
			return 0;
		}
		return (int) Math.round(value * (double) factor);
	}

	private record IngredientDraft(Long alimentoId, double cantSugerida, int pesoNeto, int pesoBruto, String unidad,
			int order) {

		private IngredientDraft mergeWith(final IngredientDraft other) {
			return new IngredientDraft(alimentoId, cantSugerida + other.cantSugerida, pesoNeto + other.pesoNeto,
					pesoBruto + other.pesoBruto, unidad, Math.min(order, other.order));
		}

		private String cantidadForAdd() {
			final IngredientePlatilloIngesta helper = new IngredientePlatilloIngesta();
			helper.setCantSugerida(cantSugerida);
			helper.setPesoNeto(pesoNeto);
			helper.setPesoBrutoRedondeado(pesoBruto);
			if (helper.shouldDisplayWeightInGrams(unidad)) {
				return String.valueOf(pesoBruto > 0 ? pesoBruto : pesoNeto);
			}
			return helper.getFractionalCantSugerida();
		}

	}

}
