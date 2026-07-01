package com.nutriconsultas.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.alimentos.AlimentosRepository;
import com.nutriconsultas.platillos.Ingrediente;
import com.nutriconsultas.util.FractionQuantityParser;
import com.nutriconsultas.util.NutrientSummarizer;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CalculateRecipeNutrientsToolServiceImpl implements CalculateRecipeNutrientsToolService {

	static final int DEFAULT_PORTIONS = 1;

	static final int MIN_INGREDIENTS = 1;

	static final int MAX_INGREDIENTS = 40;

	static final int MAX_LABEL_LENGTH = 120;

	private final AlimentosRepository alimentosRepository;

	public CalculateRecipeNutrientsToolServiceImpl(final AlimentosRepository alimentosRepository) {
		this.alimentosRepository = alimentosRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public AiToolResult<RecipeNutrientsData> calculate(@NonNull final String nutritionistId,
			@NonNull final List<RecipeIngredientInput> ingredients, @Nullable final Integer portions,
			@Nullable final String label) {
		if (!StringUtils.hasText(nutritionistId)) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, "Sesión de nutriólogo no válida.");
		}
		if (ingredients.isEmpty() || ingredients.size() > MAX_INGREDIENTS) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION,
					"La receta debe tener entre " + MIN_INGREDIENTS + " y " + MAX_INGREDIENTS + " ingredientes.");
		}
		if (label != null && label.length() > MAX_LABEL_LENGTH) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION,
					"La etiqueta no puede superar " + MAX_LABEL_LENGTH + " caracteres.");
		}
		final int effectivePortions = portions == null ? DEFAULT_PORTIONS : portions;
		if (effectivePortions < 1) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, "Las porciones deben ser al menos 1.");
		}

		final AiToolResult<Void> ingredientValidation = validateIngredients(ingredients);
		if (!ingredientValidation.success()) {
			return AiToolResult.error(Objects.requireNonNull(ingredientValidation.errorCode()),
					Objects.requireNonNull(ingredientValidation.message()));
		}

		final Map<Long, Alimento> alimentosById = loadAlimentos(ingredients);
		for (final RecipeIngredientInput ingredient : ingredients) {
			if (!alimentosById.containsKey(ingredient.alimentoId())) {
				return AiToolResult.error(AiToolErrorCode.NOT_FOUND, "No se encontró el alimento solicitado.");
			}
		}

		final Ingrediente recipeTotals = new Ingrediente();
		NutrientSummarizer.resetNutrients(recipeTotals);
		final List<RecipeIngredientNutrientResult> ingredientResults = new ArrayList<>();
		for (final RecipeIngredientInput ingredient : ingredients) {
			final Alimento alimento = alimentosById.get(ingredient.alimentoId());
			if (!AiNutrientToolSupport.isUnitSupported(alimento, ingredient.unidad())) {
				return AiToolResult.error(AiToolErrorCode.VALIDATION,
						"La unidad solicitada no es compatible con el alimento del catálogo.");
			}
			final Ingrediente calculated = AiNutrientToolSupport.calculateIngredient(alimento,
					ingredient.cantidad().trim(), ingredient.pesoNetoG());
			if (calculated == null) {
				return AiToolResult.error(AiToolErrorCode.VALIDATION,
						"No se pudo calcular el alimento con id " + ingredient.alimentoId() + ".");
			}
			NutrientSummarizer.addNutrients(recipeTotals, calculated);
			final List<String> warnings = buildMissingNutrientWarnings(alimento);
			ingredientResults.add(new RecipeIngredientNutrientResult(ingredient.alimentoId(),
					AiNutrientToolSupport.toNutrientSummary(calculated), warnings));
		}

		final NutrientSummary nutrientsTotal = AiNutrientToolSupport.toNutrientSummary(recipeTotals);
		final NutrientSummary nutrientsPerPortion = AiNutrientToolSupport.divideNutrientSummary(nutrientsTotal,
				effectivePortions);
		final RecipeNutrientsData data = new RecipeNutrientsData(effectivePortions, ingredientResults,
				nutrientsPerPortion, nutrientsTotal);
		if (log.isInfoEnabled()) {
			log.info("AI tool calculate_recipe_nutrients ingredientCount={} portions={}", ingredients.size(),
					effectivePortions);
		}
		return AiToolResult.success(data);
	}

	private Map<Long, Alimento> loadAlimentos(final List<RecipeIngredientInput> ingredients) {
		final List<Long> ids = ingredients.stream().map(RecipeIngredientInput::alimentoId).distinct().toList();
		final Map<Long, Alimento> alimentosById = new HashMap<>();
		alimentosRepository.findAllById(ids).forEach(alimento -> alimentosById.put(alimento.getId(), alimento));
		return alimentosById;
	}

	private static AiToolResult<Void> validateIngredients(final List<RecipeIngredientInput> ingredients) {
		for (final RecipeIngredientInput ingredient : ingredients) {
			if (ingredient.alimentoId() <= 0) {
				return AiToolResult.error(AiToolErrorCode.VALIDATION, "El identificador de alimento no es válido.");
			}
			if (!StringUtils.hasText(ingredient.cantidad())) {
				return AiToolResult.error(AiToolErrorCode.VALIDATION,
						"La cantidad es obligatoria para cada ingrediente.");
			}
			if (FractionQuantityParser.parseFractionalQuantity(ingredient.cantidad().trim()) == null) {
				return AiToolResult.error(AiToolErrorCode.VALIDATION, "La cantidad no es válida.");
			}
			if (ingredient.pesoNetoG() != null && ingredient.pesoNetoG() < 1) {
				return AiToolResult.error(AiToolErrorCode.VALIDATION, "El peso neto debe ser al menos 1 g.");
			}
		}
		return AiToolResult.success(null);
	}

	private static List<String> buildMissingNutrientWarnings(final Alimento alimento) {
		final List<String> warnings = new ArrayList<>();
		if (alimento.getCantSugerida() == null) {
			warnings.add("El alimento " + alimento.getId() + " no tiene cantidad sugerida en el catálogo.");
		}
		if (alimento.getEnergia() == null || alimento.getEnergia() == 0) {
			warnings.add("El alimento " + alimento.getId() + " no tiene energía registrada en el catálogo.");
		}
		if (alimento.getProteina() == null || alimento.getProteina() == 0.0) {
			warnings.add("El alimento " + alimento.getId() + " no tiene proteína registrada en el catálogo.");
		}
		if (alimento.getFibra() == null || alimento.getFibra() == 0.0) {
			warnings.add("El alimento " + alimento.getId() + " no tiene fibra registrada en el catálogo.");
		}
		if (alimento.getSodio() == null || alimento.getSodio() == 0.0) {
			warnings.add("El alimento " + alimento.getId() + " no tiene sodio registrado en el catálogo.");
		}
		return warnings;
	}

}
