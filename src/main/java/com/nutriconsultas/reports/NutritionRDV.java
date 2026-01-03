package com.nutriconsultas.reports;

/**
 * Recommended Daily Values (RDV) for nutritional analysis.
 *
 * <p>
 * These values represent standard recommended daily intakes for adults based on common
 * nutritional guidelines. Values are in standard units (grams, milligrams, micrograms,
 * etc.) per day.
 *
 * <p>
 * Note: These are general guidelines and may need to be adjusted based on individual
 * factors such as age, gender, activity level, and health conditions.
 */
public final class NutritionRDV {

	private NutritionRDV() {
		// Utility class - prevent instantiation
	}

	// Macronutrients (grams per day)
	/** Recommended daily protein intake in grams */
	public static final double PROTEIN_GRAMS = 50.0;

	/** Recommended daily lipids (fats) intake in grams */
	public static final double LIPIDS_GRAMS = 65.0;

	/** Recommended daily carbohydrates intake in grams */
	public static final double CARBOHYDRATES_GRAMS = 300.0;

	/** Recommended daily fiber intake in grams */
	public static final double FIBER_GRAMS = 25.0;

	// Energy (kilocalories per day)
	/** Recommended daily energy intake in kilocalories */
	public static final int ENERGY_KCAL = 2000;

	// Vitamins
	/** Recommended daily Vitamin A intake in micrograms (μg) */
	public static final double VITAMIN_A_MICROGRAMS = 900.0;

	/** Recommended daily Vitamin C (Ascorbic Acid) intake in milligrams (mg) */
	public static final double VITAMIN_C_MILLIGRAMS = 90.0;

	/** Recommended daily Folic Acid intake in micrograms (μg) */
	public static final double FOLIC_ACID_MICROGRAMS = 400.0;

	// Minerals
	/** Recommended daily Calcium intake in milligrams (mg) */
	public static final double CALCIUM_MILLIGRAMS = 1000.0;

	/** Recommended daily Iron intake in milligrams (mg) */
	public static final double IRON_MILLIGRAMS = 18.0;

	/** Recommended daily Sodium intake in milligrams (mg) - maximum recommended */
	public static final double SODIUM_MILLIGRAMS = 2300.0;

	/** Recommended daily Potassium intake in milligrams (mg) */
	public static final double POTASSIUM_MILLIGRAMS = 3500.0;

	/** Recommended daily Phosphorus intake in milligrams (mg) */
	public static final double PHOSPHORUS_MILLIGRAMS = 700.0;

	/** Recommended daily Selenium intake in micrograms (μg) */
	public static final double SELENIUM_MICROGRAMS = 55.0;

	// Other nutrients
	/** Recommended daily Cholesterol intake in milligrams (mg) - maximum recommended */
	public static final double CHOLESTEROL_MILLIGRAMS = 300.0;

	/** Recommended daily Saturated Fat intake in grams - maximum recommended */
	public static final double SATURATED_FAT_GRAMS = 20.0;

	/** Recommended daily Sugar intake in grams - maximum recommended */
	public static final double SUGAR_GRAMS = 50.0;

	// Tolerable Upper Intake Levels (UL) - maximum safe intake
	/** Maximum safe daily Sodium intake in milligrams (mg) */
	public static final double SODIUM_UL_MILLIGRAMS = 2300.0;

	/** Maximum safe daily Cholesterol intake in milligrams (mg) */
	public static final double CHOLESTEROL_UL_MILLIGRAMS = 300.0;

	/** Maximum safe daily Saturated Fat intake in grams */
	public static final double SATURATED_FAT_UL_GRAMS = 20.0;

	/** Maximum safe daily Sugar intake in grams */
	public static final double SUGAR_UL_GRAMS = 50.0;

}
