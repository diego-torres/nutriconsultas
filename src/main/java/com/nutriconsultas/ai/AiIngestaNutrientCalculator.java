package com.nutriconsultas.ai;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.alimentos.AlimentosRepository;
import com.nutriconsultas.platillos.Platillo;
import com.nutriconsultas.platillos.PlatilloCatalogConstants;
import com.nutriconsultas.platillos.PlatilloRepository;

/**
 * Computes nutrient totals from AI plan draft structures.
 */
@Component
public class AiIngestaNutrientCalculator {

	private final AlimentosRepository alimentosRepository;

	private final PlatilloRepository platilloRepository;

	private final CalculateRecipeNutrientsToolService recipeNutrientsToolService;

	public AiIngestaNutrientCalculator(final AlimentosRepository alimentosRepository,
			final PlatilloRepository platilloRepository,
			final CalculateRecipeNutrientsToolService recipeNutrientsToolService) {
		this.alimentosRepository = alimentosRepository;
		this.platilloRepository = platilloRepository;
		this.recipeNutrientsToolService = recipeNutrientsToolService;
	}

	public AiToolResult<IngestaNutrientComputation> computeDish(@NonNull final String nutritionistId,
			@NonNull final DishPlanInput dish) {
		if (dish.ingredients() == null || dish.ingredients().isEmpty()) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, "El platillo debe incluir ingredientes.");
		}
		final AiToolResult<RecipeNutrientsData> recipeResult = recipeNutrientsToolService.calculate(nutritionistId,
				dish.ingredients(), dish.portions(), null);
		if (!recipeResult.success()) {
			return AiToolResult.error(Objects.requireNonNull(recipeResult.errorCode()),
					Objects.requireNonNull(recipeResult.message()));
		}
		final RecipeNutrientsData recipeData = Objects.requireNonNull(recipeResult.data());
		final List<PlanConstraintWarning> warnings = new ArrayList<>();
		for (final RecipeIngredientNutrientResult ingredientResult : recipeData.ingredientResults()) {
			for (final String warning : ingredientResult.warnings()) {
				warnings.add(new PlanConstraintWarning(PlanConstraintWarningCode.MISSING_NUTRIENT_DATA, warning,
						PlanConstraintWarningSeverity.WARNING));
			}
		}
		final Set<Long> alimentoIds = new HashSet<>();
		dish.ingredients().forEach(ingredient -> alimentoIds.add(ingredient.alimentoId()));
		return AiToolResult
			.success(new IngestaNutrientComputation(recipeData.nutrientsPerPortion(), warnings, alimentoIds));
	}

	public AiToolResult<IngestaNutrientComputation> computeIngestas(@NonNull final String nutritionistId,
			@Nullable final List<IngestaSlotInput> ingestas) {
		if (ingestas == null || ingestas.isEmpty()) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, "El plan debe incluir al menos una ingesta.");
		}
		NutrientSummary totals = AiNutrientToolSupport.emptyNutrientSummary();
		final List<PlanConstraintWarning> warnings = new ArrayList<>();
		final Set<Long> alimentoIds = new HashSet<>();
		for (final IngestaSlotInput ingesta : ingestas) {
			if (ingesta.items() == null) {
				continue;
			}
			for (final IngestaSlotItemInput item : ingesta.items()) {
				final AiToolResult<ItemNutrientContribution> itemResult = computeItem(nutritionistId, item);
				if (!itemResult.success()) {
					return AiToolResult.error(Objects.requireNonNull(itemResult.errorCode()),
							Objects.requireNonNull(itemResult.message()));
				}
				final ItemNutrientContribution contribution = Objects.requireNonNull(itemResult.data());
				totals = AiNutrientToolSupport.addNutrientSummaries(totals, contribution.nutrients());
				warnings.addAll(contribution.warnings());
				alimentoIds.addAll(contribution.alimentoIds());
			}
		}
		return AiToolResult.success(new IngestaNutrientComputation(totals, warnings, alimentoIds));
	}

	private AiToolResult<ItemNutrientContribution> computeItem(final String nutritionistId,
			final IngestaSlotItemInput item) {
		final String type = item.type() != null ? item.type().trim().toUpperCase(Locale.ROOT) : "";
		return switch (type) {
			case "PLATILLO" -> computePlatilloItem(nutritionistId, item);
			case "ALIMENTO" -> computeAlimentoItem(item);
			case "RECIPE" -> computeRecipeItem(nutritionistId, item);
			default -> AiToolResult.error(AiToolErrorCode.VALIDATION, "Tipo de ítem de ingesta no válido.");
		};
	}

	private AiToolResult<ItemNutrientContribution> computePlatilloItem(final String nutritionistId,
			final IngestaSlotItemInput item) {
		if (item.platilloId() == null || item.platilloId() <= 0) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, "El platillo no es válido.");
		}
		final Platillo platillo = platilloRepository.findById(item.platilloId()).orElse(null);
		if (platillo == null || !isAuthorizedPlatillo(platillo, nutritionistId)) {
			return AiToolResult.error(AiToolErrorCode.NOT_FOUND, "No se encontró el platillo solicitado.");
		}
		final int portions = resolvePortions(item.portions());
		final NutrientSummary nutrients = AiNutrientToolSupport.platilloNutrients(platillo, portions);
		final Set<Long> alimentoIds = new HashSet<>();
		if (platillo.getIngredientes() != null) {
			platillo.getIngredientes()
				.stream()
				.filter(ingrediente -> ingrediente.getAlimento() != null)
				.forEach(ingrediente -> alimentoIds.add(ingrediente.getAlimento().getId()));
		}
		return AiToolResult.success(new ItemNutrientContribution(nutrients, List.of(), alimentoIds));
	}

	private AiToolResult<ItemNutrientContribution> computeAlimentoItem(final IngestaSlotItemInput item) {
		if (item.alimentoId() == null || item.alimentoId() <= 0) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, "El alimento no es válido.");
		}
		final Alimento alimento = alimentosRepository.findById(item.alimentoId()).orElse(null);
		if (alimento == null) {
			return AiToolResult.error(AiToolErrorCode.NOT_FOUND, "No se encontró el alimento solicitado.");
		}
		final int portions = resolvePortions(item.portions());
		final String cantidad = alimento.getFractionalCantSugerida();
		if (!StringUtils.hasText(cantidad)) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION,
					"El alimento " + alimento.getId() + " no tiene cantidad sugerida en el catálogo.");
		}
		final com.nutriconsultas.platillos.Ingrediente calculated = AiNutrientToolSupport.calculateIngredient(alimento,
				cantidad, alimento.getPesoNeto());
		if (calculated == null) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION,
					"No se pudo calcular el alimento con id " + alimento.getId() + ".");
		}
		final NutrientSummary nutrients = AiNutrientToolSupport
			.scaleNutrientSummary(AiNutrientToolSupport.toNutrientSummary(calculated), portions);
		final List<PlanConstraintWarning> warnings = buildMissingDataWarnings(alimento);
		return AiToolResult.success(new ItemNutrientContribution(nutrients, warnings, Set.of(alimento.getId())));
	}

	private AiToolResult<ItemNutrientContribution> computeRecipeItem(final String nutritionistId,
			final IngestaSlotItemInput item) {
		if (item.ingredients() == null || item.ingredients().isEmpty()) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, "La receta inline debe incluir ingredientes.");
		}
		final AiToolResult<RecipeNutrientsData> recipeResult = recipeNutrientsToolService.calculate(nutritionistId,
				item.ingredients(), item.portions(), null);
		if (!recipeResult.success()) {
			return AiToolResult.error(Objects.requireNonNull(recipeResult.errorCode()),
					Objects.requireNonNull(recipeResult.message()));
		}
		final RecipeNutrientsData recipeData = Objects.requireNonNull(recipeResult.data());
		final List<PlanConstraintWarning> warnings = new ArrayList<>();
		final Set<Long> alimentoIds = new HashSet<>();
		for (final RecipeIngredientNutrientResult ingredientResult : recipeData.ingredientResults()) {
			alimentoIds.add(ingredientResult.alimentoId());
			for (final String warning : ingredientResult.warnings()) {
				warnings.add(new PlanConstraintWarning(PlanConstraintWarningCode.MISSING_NUTRIENT_DATA, warning,
						PlanConstraintWarningSeverity.WARNING));
			}
		}
		return AiToolResult.success(new ItemNutrientContribution(recipeData.nutrientsTotal(), warnings, alimentoIds));
	}

	private static List<PlanConstraintWarning> buildMissingDataWarnings(final Alimento alimento) {
		final List<PlanConstraintWarning> warnings = new ArrayList<>();
		if (alimento.getEnergia() == null || alimento.getEnergia() == 0) {
			warnings.add(new PlanConstraintWarning(PlanConstraintWarningCode.MISSING_NUTRIENT_DATA,
					"El alimento " + alimento.getId() + " no tiene energía registrada en el catálogo.",
					PlanConstraintWarningSeverity.WARNING));
		}
		return warnings;
	}

	private static boolean isAuthorizedPlatillo(final Platillo platillo, final String nutritionistId) {
		return PlatilloCatalogConstants.isSystemCatalog(platillo)
				|| Objects.equals(nutritionistId, platillo.getUserId());
	}

	private static int resolvePortions(@Nullable final Integer portions) {
		if (portions == null || portions < 1) {
			return 1;
		}
		return portions;
	}

	record IngestaNutrientComputation(NutrientSummary nutrients, List<PlanConstraintWarning> warnings,
			Set<Long> alimentoIds) {
	}

	private record ItemNutrientContribution(NutrientSummary nutrients, List<PlanConstraintWarning> warnings,
			Set<Long> alimentoIds) {
	}

}
