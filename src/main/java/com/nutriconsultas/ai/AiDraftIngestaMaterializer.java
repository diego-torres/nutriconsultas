package com.nutriconsultas.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.alimentos.AlimentosRepository;
import com.nutriconsultas.dieta.AlimentoIngesta;
import com.nutriconsultas.dieta.DietaService;
import com.nutriconsultas.dieta.IngredientePlatilloIngesta;
import com.nutriconsultas.dieta.Ingesta;
import com.nutriconsultas.dieta.PlatilloIngesta;
import com.nutriconsultas.dieta.PlatilloIngestaMapping;
import com.nutriconsultas.platillos.Ingrediente;
import com.nutriconsultas.platillos.Platillo;
import com.nutriconsultas.platillos.PlatilloCatalogConstants;
import com.nutriconsultas.platillos.PlatilloRepository;

/**
 * Maps AI draft ingesta slot items into persisted dieta structures.
 */
@Component
public class AiDraftIngestaMaterializer {

	private final AlimentosRepository alimentosRepository;

	private final PlatilloRepository platilloRepository;

	private final DietaService dietaService;

	public AiDraftIngestaMaterializer(final AlimentosRepository alimentosRepository,
			final PlatilloRepository platilloRepository, final DietaService dietaService) {
		this.alimentosRepository = alimentosRepository;
		this.platilloRepository = platilloRepository;
		this.dietaService = dietaService;
	}

	public void addItemsToIngesta(@NonNull final Ingesta ingesta, @NonNull final List<IngestaSlotItemInput> items,
			@NonNull final String nutritionistId) {
		for (final IngestaSlotItemInput item : items) {
			final String type = item.type() != null ? item.type().trim().toUpperCase(Locale.ROOT) : "";
			switch (type) {
				case "PLATILLO" -> addPlatilloItem(ingesta, item, nutritionistId);
				case "ALIMENTO" -> addAlimentoItem(ingesta, item);
				case "RECIPE" -> addRecipeItem(ingesta, item);
				default -> throw new AiDraftLifecycleException("Tipo de ítem de ingesta no válido.");
			}
		}
	}

	private void addPlatilloItem(final Ingesta ingesta, final IngestaSlotItemInput item, final String nutritionistId) {
		if (item.platilloId() == null || item.platilloId() <= 0) {
			throw new AiDraftLifecycleException("El platillo no es válido.");
		}
		final Platillo platillo = platilloRepository.findById(item.platilloId()).orElse(null);
		if (platillo == null || !isAuthorizedPlatillo(platillo, nutritionistId)) {
			throw new AiDraftLifecycleException("No se encontró el platillo solicitado.");
		}
		final PlatilloIngesta platilloIngesta = PlatilloIngestaMapping.mapPlatilloIngesta(platillo);
		platilloIngesta.setIngesta(ingesta);
		final int portions = resolvePortions(item.portions());
		platilloIngesta.setPortions(portions);
		if (platillo.getIngredientes() != null) {
			for (final Ingrediente ingrediente : platillo.getIngredientes()) {
				final IngredientePlatilloIngesta mapped = PlatilloIngestaMapping
					.mapFromIngredienteToIngredientePlatilloIngesta(ingrediente);
				mapped.setPlatillo(platilloIngesta);
				platilloIngesta.getIngredientes().add(mapped);
			}
		}
		if (portions != 1) {
			dietaService.recalculatePlatilloIngestaNutrients(platilloIngesta, portions);
		}
		ingesta.getPlatillos().add(platilloIngesta);
	}

	private void addAlimentoItem(final Ingesta ingesta, final IngestaSlotItemInput item) {
		if (item.alimentoId() == null || item.alimentoId() <= 0) {
			throw new AiDraftLifecycleException("El alimento no es válido.");
		}
		final Alimento alimento = alimentosRepository.findById(item.alimentoId()).orElse(null);
		if (alimento == null) {
			throw new AiDraftLifecycleException("No se encontró el alimento solicitado.");
		}
		final AlimentoIngesta alimentoIngesta = mapAlimentoIngesta(alimento, resolvePortions(item.portions()));
		alimentoIngesta.setIngesta(ingesta);
		ingesta.getAlimentos().add(alimentoIngesta);
	}

	private void addRecipeItem(final Ingesta ingesta, final IngestaSlotItemInput item) {
		if (item.ingredients() == null || item.ingredients().isEmpty()) {
			throw new AiDraftLifecycleException("La receta inline debe incluir ingredientes.");
		}
		final PlatilloIngesta platilloIngesta = new PlatilloIngesta();
		platilloIngesta.setName("Receta IA");
		platilloIngesta.setIngesta(ingesta);
		platilloIngesta.setPortions(resolvePortions(item.portions()));
		for (final RecipeIngredientInput ingredientInput : item.ingredients()) {
			final Ingrediente calculated = buildCatalogIngrediente(ingredientInput);
			final IngredientePlatilloIngesta mapped = PlatilloIngestaMapping
				.mapFromIngredienteToIngredientePlatilloIngesta(calculated);
			mapped.setPlatillo(platilloIngesta);
			platilloIngesta.getIngredientes().add(mapped);
		}
		dietaService.recalculatePlatilloIngestaFromIngredientes(platilloIngesta);
		ingesta.getPlatillos().add(platilloIngesta);
	}

	private Ingrediente buildCatalogIngrediente(final RecipeIngredientInput ingredientInput) {
		final Alimento alimento = alimentosRepository.findById(ingredientInput.alimentoId()).orElse(null);
		if (alimento == null) {
			throw new AiDraftLifecycleException("No se encontró el alimento solicitado.");
		}
		if (!AiNutrientToolSupport.isUnitSupported(alimento, ingredientInput.unidad())) {
			throw new AiDraftLifecycleException("Unidad no compatible para el alimento " + alimento.getId() + ".");
		}
		final Integer pesoNeto = ingredientInput.pesoNetoG() != null ? ingredientInput.pesoNetoG()
				: alimento.getPesoNeto();
		if (pesoNeto == null) {
			throw new AiDraftLifecycleException(
					"El alimento " + alimento.getId() + " no tiene peso neto en el catálogo.");
		}
		final Ingrediente calculated = AiNutrientToolSupport.calculateIngredient(alimento,
				ingredientInput.cantidad().trim(), pesoNeto);
		if (calculated == null) {
			throw new AiDraftLifecycleException("No se pudo calcular el alimento con id " + alimento.getId() + ".");
		}
		return calculated;
	}

	private static AlimentoIngesta mapAlimentoIngesta(final Alimento alimento, final int portions) {
		final AlimentoIngesta alimentoIngesta = new AlimentoIngesta();
		alimentoIngesta.setName(alimento.getNombreAlimento());
		alimentoIngesta.setAlimento(alimento);
		alimentoIngesta.setPortions(portions);
		alimentoIngesta.setUnidad(alimento.getUnidad());
		if (alimento.getEnergia() != null) {
			alimentoIngesta.setEnergia(alimento.getEnergia() * portions);
		}
		if (alimento.getProteina() != null) {
			alimentoIngesta.setProteina(alimento.getProteina() * portions);
		}
		if (alimento.getLipidos() != null) {
			alimentoIngesta.setLipidos(alimento.getLipidos() * portions);
		}
		if (alimento.getHidratosDeCarbono() != null) {
			alimentoIngesta.setHidratosDeCarbono(alimento.getHidratosDeCarbono() * portions);
		}
		if (alimento.getPesoBrutoRedondeado() != null) {
			alimentoIngesta.setPesoBrutoRedondeado(alimento.getPesoBrutoRedondeado() * portions);
		}
		if (alimento.getPesoNeto() != null) {
			alimentoIngesta.setPesoNeto(alimento.getPesoNeto() * portions);
		}
		if (alimento.getFibra() != null) {
			alimentoIngesta.setFibra(alimento.getFibra() * portions);
		}
		if (alimento.getSodio() != null) {
			alimentoIngesta.setSodio(alimento.getSodio() * portions);
		}
		if (alimento.getPotasio() != null) {
			alimentoIngesta.setPotasio(alimento.getPotasio() * portions);
		}
		return alimentoIngesta;
	}

	private static boolean isAuthorizedPlatillo(final Platillo platillo, final String nutritionistId) {
		return PlatilloCatalogConstants.isSystemCatalog(platillo)
				|| Objects.equals(nutritionistId, platillo.getUserId());
	}

	private static int resolvePortions(final Integer portions) {
		if (portions == null || portions < 1) {
			return 1;
		}
		return portions;
	}

	public static Ingesta buildIngesta(final String nombre, final int orden) {
		final Ingesta ingesta = new Ingesta();
		ingesta.setNombre(nombre);
		ingesta.setOrden(orden);
		ingesta.setPlatillos(new ArrayList<>());
		ingesta.setAlimentos(new ArrayList<>());
		return ingesta;
	}

}
