package com.nutriconsultas.ai;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nutriconsultas.alimentos.Alimento;
import com.nutriconsultas.alimentos.AlimentosRepository;
import com.nutriconsultas.platillos.Ingrediente;
import com.nutriconsultas.util.FractionQuantityParser;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GetFoodNutrientsToolServiceImpl implements GetFoodNutrientsToolService {

	static final int DEFAULT_PORTIONS = 1;

	private final AlimentosRepository alimentosRepository;

	public GetFoodNutrientsToolServiceImpl(final AlimentosRepository alimentosRepository) {
		this.alimentosRepository = alimentosRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public AiToolResult<FoodNutrientsData> getNutrients(@NonNull final String nutritionistId, final long alimentoId,
			@NonNull final String cantidad, @Nullable final Integer pesoNetoG, @Nullable final Integer portions,
			@Nullable final String unidad) {
		if (!StringUtils.hasText(nutritionistId)) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, "Sesión de nutriólogo no válida.");
		}
		if (!StringUtils.hasText(cantidad)) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, "La cantidad es obligatoria.");
		}
		final String trimmedCantidad = cantidad.trim();
		if (FractionQuantityParser.parseFractionalQuantity(trimmedCantidad) == null) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, "La cantidad no es válida.");
		}
		if (pesoNetoG != null && pesoNetoG < 1) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, "El peso neto debe ser al menos 1 g.");
		}
		final int effectivePortions = portions == null ? DEFAULT_PORTIONS : portions;
		if (effectivePortions < 1) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, "Las porciones deben ser al menos 1.");
		}

		final Alimento alimento = alimentosRepository.findById(alimentoId).orElse(null);
		if (alimento == null) {
			return AiToolResult.error(AiToolErrorCode.NOT_FOUND, "No se encontró el alimento solicitado.");
		}
		if (!AiNutrientToolSupport.isUnitSupported(alimento, unidad)) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION,
					"La unidad solicitada no es compatible con el alimento del catálogo.");
		}

		final Ingrediente calculated = AiNutrientToolSupport.calculateIngredient(alimento, trimmedCantidad, pesoNetoG);
		if (calculated == null) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, "No se pudo calcular el alimento con esos datos.");
		}
		final NutrientSummary perCalculation = AiNutrientToolSupport.toNutrientSummary(calculated);
		final NutrientSummary total = AiNutrientToolSupport.scaleNutrientSummary(perCalculation, effectivePortions);
		final FoodNutrientsData data = new FoodNutrientsData(alimento.getId(), alimento.getNombreAlimento(),
				trimmedCantidad, calculated.getPesoNeto(), perCalculation, total);
		if (log.isInfoEnabled()) {
			log.info("AI tool get_food_nutrients alimentoId={} portions={}", alimentoId, effectivePortions);
		}
		return AiToolResult.success(data);
	}

}
