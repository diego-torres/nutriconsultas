package com.nutriconsultas.reports;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of nutrition analysis for a dietary plan.
 *
 * <p>
 * Contains comprehensive nutritional analysis including total nutrients, comparisons
 * against recommended daily values, identified deficiencies and excesses, and
 * recommendations for improvement.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NutritionAnalysisResult {

	/** The diet being analyzed */
	private DietaSummary dieta;

	/** Total nutrients in the diet */
	private NutrientTotals totals;

	/** Nutrients that are below recommended levels */
	private List<NutrientDeficiency> deficiencies = new ArrayList<>();

	/** Nutrients that exceed recommended levels */
	private List<NutrientExcess> excesses = new ArrayList<>();

	/** Macro and micronutrient distribution analysis */
	private NutrientDistribution distribution;

	/** Recommendations for improving the diet */
	private List<String> recommendations = new ArrayList<>();

	/**
	 * Summary information about the diet being analyzed.
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class DietaSummary {

		private Long id;

		private String nombre;

	}

	/**
	 * Total nutrient values calculated from the diet.
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class NutrientTotals {

		// Macronutrients (grams)
		private Double proteina;

		private Double lipidos;

		private Double hidratosDeCarbono;

		private Double fibra;

		// Energy (kcal)
		private Integer energia;

		// Vitamins
		private Double vitA; // micrograms

		private Double acidoAscorbico; // milligrams (Vitamin C)

		private Double acidoFolico; // micrograms

		// Minerals
		private Double calcio; // milligrams

		private Double hierro; // milligrams

		private Double sodio; // milligrams

		private Double potasio; // milligrams

		private Double fosforo; // milligrams

		private Double selenio; // micrograms

		// Other
		private Double colesterol; // milligrams

		private Double agSaturados; // grams (saturated fats)

		private Double azucarPorEquivalente; // grams (sugar)

	}

	/**
	 * Represents a nutrient deficiency (below recommended level).
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class NutrientDeficiency {

		private String nutrientName;

		private Double actualValue;

		private Double recommendedValue;

		private String unit;

		private Double percentageOfRDV;

		private String recommendation;

	}

	/**
	 * Represents a nutrient excess (above recommended/tolerable level).
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class NutrientExcess {

		private String nutrientName;

		private Double actualValue;

		private Double recommendedValue;

		private String unit;

		private Double percentageOfRDV;

		private String recommendation;

	}

	/**
	 * Macro and micronutrient distribution analysis.
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class NutrientDistribution {

		/** Percentage of calories from protein */
		private Double proteinPercentage;

		/** Percentage of calories from lipids (fats) */
		private Double lipidsPercentage;

		/** Percentage of calories from carbohydrates */
		private Double carbohydratesPercentage;

		/** Recommended protein percentage range (e.g., 10-35%) */
		private String recommendedProteinRange = "10-35%";

		/** Recommended lipids percentage range (e.g., 20-35%) */
		private String recommendedLipidsRange = "20-35%";

		/** Recommended carbohydrates percentage range (e.g., 45-65%) */
		private String recommendedCarbohydratesRange = "45-65%";

	}

}
