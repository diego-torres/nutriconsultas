package com.nutriconsultas.reports;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import com.nutriconsultas.dieta.AlimentoIngesta;
import com.nutriconsultas.dieta.Dieta;
import com.nutriconsultas.dieta.DietaService;
import com.nutriconsultas.dieta.Ingesta;
import com.nutriconsultas.dieta.PlatilloIngesta;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for analyzing nutritional content of dietary plans.
 *
 * <p>
 * This service provides comprehensive nutritional analysis including:
 * <ul>
 * <li>Calculation of total nutrients from dietary plans</li>
 * <li>Comparison against recommended daily values (RDV)</li>
 * <li>Identification of nutritional deficiencies and excesses</li>
 * <li>Macro and micronutrient distribution analysis</li>
 * <li>Recommendations for dietary improvements</li>
 * </ul>
 *
 * @see NutritionRDV
 * @see NutritionAnalysisResult
 */
@Service
@Slf4j
public class NutritionAnalysisService {

	@Autowired
	private DietaService dietaService;

	/**
	 * Analyzes the nutritional content of a dietary plan.
	 * @param dietaId the ID of the diet to analyze
	 * @param userId the user ID to verify diet ownership
	 * @return comprehensive nutrition analysis result
	 * @throws IllegalArgumentException if diet not found or access denied
	 */
	public NutritionAnalysisResult analyzeDiet(@NonNull final Long dietaId, @NonNull final String userId) {
		log.info("Analyzing nutrition for dieta id: {} (user: {})", dietaId, userId);

		final Dieta dieta = dietaService.getDietaByIdAndUserId(dietaId, userId);
		if (dieta == null) {
			throw new IllegalArgumentException("Diet with id " + dietaId + " not found or access denied");
		}

		// Calculate total nutrients
		final NutritionAnalysisResult.NutrientTotals totals = calculateTotalNutrients(dieta);

		// Create result object
		final NutritionAnalysisResult result = new NutritionAnalysisResult();
		final NutritionAnalysisResult.DietaSummary summary = new NutritionAnalysisResult.DietaSummary();
		summary.setId(dieta.getId());
		summary.setNombre(dieta.getNombre());
		result.setDieta(summary);
		result.setTotals(totals);

		// Analyze deficiencies and excesses
		analyzeDeficiencies(totals, result);
		analyzeExcesses(totals, result);

		// Calculate macro distribution
		result.setDistribution(calculateMacroDistribution(totals));

		// Generate recommendations
		generateRecommendations(result);

		log.info("Completed nutrition analysis for dieta id: {}", dietaId);
		return result;
	}

	/**
	 * Calculates total nutrients from all ingestas in the diet.
	 */
	private NutritionAnalysisResult.NutrientTotals calculateTotalNutrients(final Dieta dieta) {
		final NutritionAnalysisResult.NutrientTotals totals = new NutritionAnalysisResult.NutrientTotals();

		if (dieta.getIngestas() == null || dieta.getIngestas().isEmpty()) {
			return totals;
		}

		for (final Ingesta ingesta : dieta.getIngestas()) {
			// Sum from platillos
			if (ingesta.getPlatillos() != null) {
				for (final PlatilloIngesta platillo : ingesta.getPlatillos()) {
					addNutrientsFromPlatillo(platillo, totals);
				}
			}

			// Sum from alimentos
			if (ingesta.getAlimentos() != null) {
				for (final AlimentoIngesta alimento : ingesta.getAlimentos()) {
					addNutrientsFromAlimento(alimento, totals);
				}
			}
		}

		return totals;
	}

	/**
	 * Adds an Integer value to the current total, handling null values.
	 * @param current the current total value (may be null)
	 * @param toAdd the value to add (may be null)
	 * @return the sum of current and toAdd, or null if both are null
	 */
	private Integer addIntegerValue(final Integer current, final Integer toAdd) {
		if (toAdd == null) {
			return current;
		}
		return (current != null ? current : 0) + toAdd;
	}

	/**
	 * Adds a Double value to the current total, handling null values.
	 * @param current the current total value (may be null)
	 * @param toAdd the value to add (may be null)
	 * @return the sum of current and toAdd, or null if both are null
	 */
	private Double addDoubleValue(final Double current, final Double toAdd) {
		if (toAdd == null) {
			return current;
		}
		return (current != null ? current : 0.0) + toAdd;
	}

	/**
	 * Adds nutrients from a platillo to the totals.
	 */
	private void addNutrientsFromPlatillo(final PlatilloIngesta platillo,
			final NutritionAnalysisResult.NutrientTotals totals) {
		totals.setEnergia(addIntegerValue(totals.getEnergia(), platillo.getEnergia()));
		totals.setProteina(addDoubleValue(totals.getProteina(), platillo.getProteina()));
		totals.setLipidos(addDoubleValue(totals.getLipidos(), platillo.getLipidos()));
		totals.setHidratosDeCarbono(addDoubleValue(totals.getHidratosDeCarbono(), platillo.getHidratosDeCarbono()));
		totals.setFibra(addDoubleValue(totals.getFibra(), platillo.getFibra()));
		totals.setVitA(addDoubleValue(totals.getVitA(), platillo.getVitA()));
		totals.setAcidoAscorbico(addDoubleValue(totals.getAcidoAscorbico(), platillo.getAcidoAscorbico()));
		totals.setAcidoFolico(addDoubleValue(totals.getAcidoFolico(), platillo.getAcidoFolico()));
		totals.setCalcio(addDoubleValue(totals.getCalcio(), platillo.getCalcio()));
		totals.setHierro(addDoubleValue(totals.getHierro(), platillo.getHierro()));
		totals.setSodio(addDoubleValue(totals.getSodio(), platillo.getSodio()));
		totals.setPotasio(addDoubleValue(totals.getPotasio(), platillo.getPotasio()));
		totals.setFosforo(addDoubleValue(totals.getFosforo(), platillo.getFosforo()));
		totals.setSelenio(addDoubleValue(totals.getSelenio(), platillo.getSelenio()));
		totals.setColesterol(addDoubleValue(totals.getColesterol(), platillo.getColesterol()));
		totals.setAgSaturados(addDoubleValue(totals.getAgSaturados(), platillo.getAgSaturados()));
		totals.setAzucarPorEquivalente(
				addDoubleValue(totals.getAzucarPorEquivalente(), platillo.getAzucarPorEquivalente()));
	}

	/**
	 * Adds nutrients from an alimento to the totals.
	 */
	private void addNutrientsFromAlimento(final AlimentoIngesta alimento,
			final NutritionAnalysisResult.NutrientTotals totals) {
		totals.setEnergia(addIntegerValue(totals.getEnergia(), alimento.getEnergia()));
		totals.setProteina(addDoubleValue(totals.getProteina(), alimento.getProteina()));
		totals.setLipidos(addDoubleValue(totals.getLipidos(), alimento.getLipidos()));
		totals.setHidratosDeCarbono(addDoubleValue(totals.getHidratosDeCarbono(), alimento.getHidratosDeCarbono()));
		totals.setFibra(addDoubleValue(totals.getFibra(), alimento.getFibra()));
		totals.setVitA(addDoubleValue(totals.getVitA(), alimento.getVitA()));
		totals.setAcidoAscorbico(addDoubleValue(totals.getAcidoAscorbico(), alimento.getAcidoAscorbico()));
		totals.setAcidoFolico(addDoubleValue(totals.getAcidoFolico(), alimento.getAcidoFolico()));
		totals.setCalcio(addDoubleValue(totals.getCalcio(), alimento.getCalcio()));
		totals.setHierro(addDoubleValue(totals.getHierro(), alimento.getHierro()));
		totals.setSodio(addDoubleValue(totals.getSodio(), alimento.getSodio()));
		totals.setPotasio(addDoubleValue(totals.getPotasio(), alimento.getPotasio()));
		totals.setFosforo(addDoubleValue(totals.getFosforo(), alimento.getFosforo()));
		totals.setSelenio(addDoubleValue(totals.getSelenio(), alimento.getSelenio()));
		totals.setColesterol(addDoubleValue(totals.getColesterol(), alimento.getColesterol()));
		totals.setAgSaturados(addDoubleValue(totals.getAgSaturados(), alimento.getAgSaturados()));
		totals.setAzucarPorEquivalente(
				addDoubleValue(totals.getAzucarPorEquivalente(), alimento.getAzucarPorEquivalente()));
	}

	/**
	 * Analyzes nutrient deficiencies (below recommended levels).
	 */
	private void analyzeDeficiencies(final NutritionAnalysisResult.NutrientTotals totals,
			final NutritionAnalysisResult result) {
		final List<NutritionAnalysisResult.NutrientDeficiency> deficiencies = new ArrayList<>();

		// Protein
		if (totals.getProteina() != null && totals.getProteina() < NutritionRDV.PROTEIN_GRAMS) {
			final double percentage = (totals.getProteina() / NutritionRDV.PROTEIN_GRAMS) * 100.0;
			deficiencies.add(new NutritionAnalysisResult.NutrientDeficiency("Proteína", totals.getProteina(),
					NutritionRDV.PROTEIN_GRAMS, "g", percentage,
					"Considere aumentar el consumo de proteínas magras como pollo, pescado, legumbres y productos lácteos."));
		}

		// Lipids
		if (totals.getLipidos() != null && totals.getLipidos() < NutritionRDV.LIPIDS_GRAMS) {
			final double percentage = (totals.getLipidos() / NutritionRDV.LIPIDS_GRAMS) * 100.0;
			deficiencies.add(new NutritionAnalysisResult.NutrientDeficiency("Lípidos", totals.getLipidos(),
					NutritionRDV.LIPIDS_GRAMS, "g", percentage,
					"Incluya fuentes de grasas saludables como aguacate, nueces, aceite de oliva y pescados grasos."));
		}

		// Carbohydrates
		if (totals.getHidratosDeCarbono() != null && totals.getHidratosDeCarbono() < NutritionRDV.CARBOHYDRATES_GRAMS) {
			final double percentage = (totals.getHidratosDeCarbono() / NutritionRDV.CARBOHYDRATES_GRAMS) * 100.0;
			deficiencies.add(new NutritionAnalysisResult.NutrientDeficiency("Hidratos de Carbono",
					totals.getHidratosDeCarbono(), NutritionRDV.CARBOHYDRATES_GRAMS, "g", percentage,
					"Aumente el consumo de carbohidratos complejos como granos integrales, frutas y verduras."));
		}

		// Fiber
		if (totals.getFibra() != null && totals.getFibra() < NutritionRDV.FIBER_GRAMS) {
			final double percentage = (totals.getFibra() / NutritionRDV.FIBER_GRAMS) * 100.0;
			deficiencies.add(new NutritionAnalysisResult.NutrientDeficiency("Fibra", totals.getFibra(),
					NutritionRDV.FIBER_GRAMS, "g", percentage,
					"Incluya más frutas, verduras, legumbres y granos integrales en su dieta."));
		}

		// Energy
		if (totals.getEnergia() != null && totals.getEnergia() < NutritionRDV.ENERGY_KCAL) {
			final double percentage = (totals.getEnergia().doubleValue() / NutritionRDV.ENERGY_KCAL) * 100.0;
			deficiencies.add(new NutritionAnalysisResult.NutrientDeficiency("Energía",
					totals.getEnergia().doubleValue(), (double) NutritionRDV.ENERGY_KCAL, "kcal", percentage,
					"Considere aumentar la ingesta calórica con alimentos nutritivos y balanceados."));
		}

		// Vitamin A
		if (totals.getVitA() != null && totals.getVitA() < NutritionRDV.VITAMIN_A_MICROGRAMS) {
			final double percentage = (totals.getVitA() / NutritionRDV.VITAMIN_A_MICROGRAMS) * 100.0;
			deficiencies.add(new NutritionAnalysisResult.NutrientDeficiency("Vitamina A", totals.getVitA(),
					NutritionRDV.VITAMIN_A_MICROGRAMS, "μg", percentage,
					"Incluya alimentos ricos en vitamina A como zanahorias, espinacas, batatas y hígado."));
		}

		// Vitamin C
		if (totals.getAcidoAscorbico() != null && totals.getAcidoAscorbico() < NutritionRDV.VITAMIN_C_MILLIGRAMS) {
			final double percentage = (totals.getAcidoAscorbico() / NutritionRDV.VITAMIN_C_MILLIGRAMS) * 100.0;
			deficiencies.add(new NutritionAnalysisResult.NutrientDeficiency("Vitamina C", totals.getAcidoAscorbico(),
					NutritionRDV.VITAMIN_C_MILLIGRAMS, "mg", percentage,
					"Aumente el consumo de frutas cítricas, fresas, pimientos y brócoli."));
		}

		// Folic Acid
		if (totals.getAcidoFolico() != null && totals.getAcidoFolico() < NutritionRDV.FOLIC_ACID_MICROGRAMS) {
			final double percentage = (totals.getAcidoFolico() / NutritionRDV.FOLIC_ACID_MICROGRAMS) * 100.0;
			deficiencies.add(new NutritionAnalysisResult.NutrientDeficiency("Ácido Fólico", totals.getAcidoFolico(),
					NutritionRDV.FOLIC_ACID_MICROGRAMS, "μg", percentage,
					"Incluya legumbres, verduras de hoja verde y cereales fortificados."));
		}

		// Calcium
		if (totals.getCalcio() != null && totals.getCalcio() < NutritionRDV.CALCIUM_MILLIGRAMS) {
			final double percentage = (totals.getCalcio() / NutritionRDV.CALCIUM_MILLIGRAMS) * 100.0;
			deficiencies.add(new NutritionAnalysisResult.NutrientDeficiency("Calcio", totals.getCalcio(),
					NutritionRDV.CALCIUM_MILLIGRAMS, "mg", percentage,
					"Aumente el consumo de productos lácteos, sardinas, tofu y verduras de hoja verde."));
		}

		// Iron
		if (totals.getHierro() != null && totals.getHierro() < NutritionRDV.IRON_MILLIGRAMS) {
			final double percentage = (totals.getHierro() / NutritionRDV.IRON_MILLIGRAMS) * 100.0;
			final String recommendation = "Incluya carnes magras, legumbres, espinacas y cereales fortificados. "
					+ "Combine con vitamina C para mejor absorción.";
			deficiencies.add(new NutritionAnalysisResult.NutrientDeficiency("Hierro", totals.getHierro(),
					NutritionRDV.IRON_MILLIGRAMS, "mg", percentage, recommendation));
		}

		// Potassium
		if (totals.getPotasio() != null && totals.getPotasio() < NutritionRDV.POTASSIUM_MILLIGRAMS) {
			final double percentage = (totals.getPotasio() / NutritionRDV.POTASSIUM_MILLIGRAMS) * 100.0;
			deficiencies.add(new NutritionAnalysisResult.NutrientDeficiency("Potasio", totals.getPotasio(),
					NutritionRDV.POTASSIUM_MILLIGRAMS, "mg", percentage,
					"Aumente el consumo de plátanos, papas, frijoles y verduras de hoja verde."));
		}

		// Phosphorus
		if (totals.getFosforo() != null && totals.getFosforo() < NutritionRDV.PHOSPHORUS_MILLIGRAMS) {
			final double percentage = (totals.getFosforo() / NutritionRDV.PHOSPHORUS_MILLIGRAMS) * 100.0;
			deficiencies.add(new NutritionAnalysisResult.NutrientDeficiency("Fósforo", totals.getFosforo(),
					NutritionRDV.PHOSPHORUS_MILLIGRAMS, "mg", percentage,
					"Incluya productos lácteos, pescado, carne y nueces en su dieta."));
		}

		// Selenium
		if (totals.getSelenio() != null && totals.getSelenio() < NutritionRDV.SELENIUM_MICROGRAMS) {
			final double percentage = (totals.getSelenio() / NutritionRDV.SELENIUM_MICROGRAMS) * 100.0;
			deficiencies.add(new NutritionAnalysisResult.NutrientDeficiency("Selenio", totals.getSelenio(),
					NutritionRDV.SELENIUM_MICROGRAMS, "μg", percentage,
					"Incluya nueces de Brasil, pescado, mariscos y huevos."));
		}

		result.setDeficiencies(deficiencies);
	}

	/**
	 * Analyzes nutrient excesses (above recommended/tolerable levels).
	 */
	private void analyzeExcesses(final NutritionAnalysisResult.NutrientTotals totals,
			final NutritionAnalysisResult result) {
		final List<NutritionAnalysisResult.NutrientExcess> excesses = new ArrayList<>();

		// Sodium
		if (totals.getSodio() != null && totals.getSodio() > NutritionRDV.SODIUM_UL_MILLIGRAMS) {
			final double percentage = (totals.getSodio() / NutritionRDV.SODIUM_UL_MILLIGRAMS) * 100.0;
			excesses.add(new NutritionAnalysisResult.NutrientExcess("Sodio", totals.getSodio(),
					NutritionRDV.SODIUM_UL_MILLIGRAMS, "mg", percentage,
					"Reduzca el consumo de alimentos procesados, enlatados y sal de mesa. Use hierbas y especias para sazonar."));
		}

		// Cholesterol
		if (totals.getColesterol() != null && totals.getColesterol() > NutritionRDV.CHOLESTEROL_UL_MILLIGRAMS) {
			final double percentage = (totals.getColesterol() / NutritionRDV.CHOLESTEROL_UL_MILLIGRAMS) * 100.0;
			final String recommendation = "Reduzca el consumo de carnes rojas, yemas de huevo y productos lácteos "
					+ "altos en grasa. Prefiera proteínas magras.";
			excesses.add(new NutritionAnalysisResult.NutrientExcess("Colesterol", totals.getColesterol(),
					NutritionRDV.CHOLESTEROL_UL_MILLIGRAMS, "mg", percentage, recommendation));
		}

		// Saturated Fats
		if (totals.getAgSaturados() != null && totals.getAgSaturados() > NutritionRDV.SATURATED_FAT_UL_GRAMS) {
			final double percentage = (totals.getAgSaturados() / NutritionRDV.SATURATED_FAT_UL_GRAMS) * 100.0;
			final String recommendation = "Reduzca el consumo de carnes grasas, mantequilla y productos lácteos "
					+ "enteros. Prefiera grasas insaturadas.";
			excesses.add(new NutritionAnalysisResult.NutrientExcess("Grasas Saturadas", totals.getAgSaturados(),
					NutritionRDV.SATURATED_FAT_UL_GRAMS, "g", percentage, recommendation));
		}

		// Sugar
		if (totals.getAzucarPorEquivalente() != null
				&& totals.getAzucarPorEquivalente() > NutritionRDV.SUGAR_UL_GRAMS) {
			final double percentage = (totals.getAzucarPorEquivalente() / NutritionRDV.SUGAR_UL_GRAMS) * 100.0;
			excesses.add(new NutritionAnalysisResult.NutrientExcess("Azúcar", totals.getAzucarPorEquivalente(),
					NutritionRDV.SUGAR_UL_GRAMS, "g", percentage,
					"Reduzca el consumo de bebidas azucaradas, dulces y alimentos procesados. Prefiera frutas naturales."));
		}

		result.setExcesses(excesses);
	}

	/**
	 * Calculates macro nutrient distribution (percentage of calories from each
	 * macronutrient).
	 */
	private NutritionAnalysisResult.NutrientDistribution calculateMacroDistribution(
			final NutritionAnalysisResult.NutrientTotals totals) {
		final NutritionAnalysisResult.NutrientDistribution distribution = new NutritionAnalysisResult.NutrientDistribution();

		// Calculate total calories from macros
		final double proteinCalories = (totals.getProteina() != null ? totals.getProteina() : 0.0) * 4.0;
		final double lipidsCalories = (totals.getLipidos() != null ? totals.getLipidos() : 0.0) * 9.0;
		final double carbsCalories = (totals.getHidratosDeCarbono() != null ? totals.getHidratosDeCarbono() : 0.0)
				* 4.0;
		final double totalCalories = proteinCalories + lipidsCalories + carbsCalories;

		if (totalCalories > 0) {
			distribution.setProteinPercentage((proteinCalories / totalCalories) * 100.0);
			distribution.setLipidsPercentage((lipidsCalories / totalCalories) * 100.0);
			distribution.setCarbohydratesPercentage((carbsCalories / totalCalories) * 100.0);
		}
		else {
			distribution.setProteinPercentage(0.0);
			distribution.setLipidsPercentage(0.0);
			distribution.setCarbohydratesPercentage(0.0);
		}

		return distribution;
	}

	/**
	 * Generates recommendations based on the analysis.
	 */
	private void generateRecommendations(final NutritionAnalysisResult result) {
		final List<String> recommendations = new ArrayList<>();

		// General recommendations based on deficiencies
		if (!result.getDeficiencies().isEmpty()) {
			recommendations.add("Se identificaron " + result.getDeficiencies().size()
					+ " nutrientes por debajo de los valores recomendados. "
					+ "Revise las deficiencias específicas y ajuste su plan alimentario.");
		}

		// Recommendations based on excesses
		if (!result.getExcesses().isEmpty()) {
			recommendations.add("Se identificaron " + result.getExcesses().size()
					+ " nutrientes que exceden los valores recomendados. "
					+ "Considere reducir estos nutrientes para mantener una dieta equilibrada.");
		}

		// Macro distribution recommendations
		final NutritionAnalysisResult.NutrientDistribution dist = result.getDistribution();
		if (dist != null) {
			if (dist.getProteinPercentage() != null && dist.getProteinPercentage() < 10.0) {
				recommendations.add("El porcentaje de calorías provenientes de proteínas es bajo (<10%). "
						+ "Considere aumentar la ingesta de proteínas.");
			}
			if (dist.getProteinPercentage() != null && dist.getProteinPercentage() > 35.0) {
				recommendations.add("El porcentaje de calorías provenientes de proteínas es alto (>35%). "
						+ "Considere balancear con más carbohidratos y grasas saludables.");
			}

			if (dist.getLipidsPercentage() != null && dist.getLipidsPercentage() < 20.0) {
				recommendations.add("El porcentaje de calorías provenientes de grasas es bajo (<20%). "
						+ "Incluya más grasas saludables en su dieta.");
			}
			if (dist.getLipidsPercentage() != null && dist.getLipidsPercentage() > 35.0) {
				recommendations.add("El porcentaje de calorías provenientes de grasas es alto (>35%). "
						+ "Considere reducir las grasas saturadas y aumentar carbohidratos complejos.");
			}

			if (dist.getCarbohydratesPercentage() != null && dist.getCarbohydratesPercentage() < 45.0) {
				recommendations.add("El porcentaje de calorías provenientes de carbohidratos es bajo (<45%). "
						+ "Aumente el consumo de carbohidratos complejos.");
			}
			if (dist.getCarbohydratesPercentage() != null && dist.getCarbohydratesPercentage() > 65.0) {
				recommendations.add("El porcentaje de calorías provenientes de carbohidratos es alto (>65%). "
						+ "Considere balancear con más proteínas y grasas saludables.");
			}
		}

		// Overall balance recommendation
		if (result.getDeficiencies().isEmpty() && result.getExcesses().isEmpty()) {
			recommendations.add("¡Excelente! Su plan alimentario cumple con los valores recomendados de nutrientes. "
					+ "Mantenga este equilibrio.");
		}

		result.setRecommendations(recommendations);
	}

}
