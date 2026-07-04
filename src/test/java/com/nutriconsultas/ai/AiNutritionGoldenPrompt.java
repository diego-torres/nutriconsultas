package com.nutriconsultas.ai;

import java.util.List;
import java.util.stream.Stream;

/**
 * Golden prompt fixtures for supported nutrition workflows (#401). Scenarios align with
 * {@code docs/ai/FUNCTIONAL-SCOPE.md} and {@code docs/ai/NUTRITION-GOLDEN-PROMPTS.md}.
 */
final class AiNutritionGoldenPrompt {

	enum Category {

		DISH, MENU_DAILY, MENU_WEEKLY, DIET_PLAN, CATALOG_LOOKUP, CLARIFY

	}

	enum Expectation {

		/**
		 * Model should call listed tools (in any order) then produce a draft or summary.
		 */
		MUST_USE_TOOLS,
		/** Model should ask a clarifying question before draft tools. */
		MUST_CLARIFY,
		/** Catalog search expected; draft tools optional when ingredient is missing. */
		MUST_SEARCH_CATALOG

	}

	record Scenario(String id, String prompt, Category category, Expectation expectation, List<String> expectedTools,
			List<String> expectedWarningFragments, String notes, boolean usesPatientContext) {
	}

	private AiNutritionGoldenPrompt() {
	}

	static Stream<Scenario> scenarios() {
		return ALL.stream();
	}

	static Stream<Scenario> mustUseToolsScenarios() {
		return ALL.stream().filter(scenario -> scenario.expectation() == Expectation.MUST_USE_TOOLS);
	}

	static Stream<Scenario> mustClarifyScenarios() {
		return ALL.stream().filter(scenario -> scenario.expectation() == Expectation.MUST_CLARIFY);
	}

	static Stream<Scenario> mustSearchCatalogScenarios() {
		return ALL.stream().filter(scenario -> scenario.expectation() == Expectation.MUST_SEARCH_CATALOG);
	}

	private static final List<Scenario> ALL = List.of(
			new Scenario("high-protein-breakfast",
					"Genera un desayuno alto en proteína usando alimentos de mi catálogo.", Category.DISH,
					Expectation.MUST_USE_TOOLS,
					List.of(SearchFoodCatalogToolService.TOOL_NAME, CalculateRecipeNutrientsToolService.TOOL_NAME,
							CreateDishDraftToolService.TOOL_NAME),
					List.of("Borrador IA", "revisión del nutriólogo"),
					"Search catalog foods, calculate nutrients, save dish draft", false),
			new Scenario("low-sodium-day-menu",
					"Menú bajo en sodio de un día: desayuno, comida, cena y dos colaciones.", Category.MENU_DAILY,
					Expectation.MUST_USE_TOOLS,
					List.of(SearchFoodCatalogToolService.TOOL_NAME, SearchDishCatalogToolService.TOOL_NAME,
							ValidatePlanConstraintsToolService.TOOL_NAME, CreateMenuDraftToolService.TOOL_NAME),
					List.of("sodio", "Borrador IA"), "Daily menu with sodium constraint validation", false),
			new Scenario("7-day-weight-loss-menu", "Menú semanal de 7 días para pérdida de peso, 1600 kcal.",
					Category.MENU_WEEKLY, Expectation.MUST_USE_TOOLS,
					List.of(SearchFoodCatalogToolService.TOOL_NAME, SearchDishCatalogToolService.TOOL_NAME,
							ValidatePlanConstraintsToolService.TOOL_NAME, CreateDietPlanDraftToolService.TOOL_NAME),
					List.of("1600", "Borrador IA"), "Seven-day plan draft within scope cap (#447)", false),
			new Scenario("egg-allergy-plan", "Plan de 7 días sin huevo para el paciente vinculado.", Category.DIET_PLAN,
					Expectation.MUST_USE_TOOLS,
					List.of(SearchFoodCatalogToolService.TOOL_NAME, ValidatePlanConstraintsToolService.TOOL_NAME,
							CreateDietPlanDraftToolService.TOOL_NAME),
					List.of("huevo", "alergia", "Borrador IA"), "Uses linked patient allergy context (#362)", true),
			new Scenario("diabetic-friendly-menu",
					"Menú de un día amigable para diabetes, 1800 kcal, bajo índice glucémico.", Category.MENU_DAILY,
					Expectation.MUST_USE_TOOLS,
					List.of(SearchFoodCatalogToolService.TOOL_NAME, SearchDishCatalogToolService.TOOL_NAME,
							ValidatePlanConstraintsToolService.TOOL_NAME, CreateMenuDraftToolService.TOOL_NAME),
					List.of("revisión profesional", "Borrador IA"),
					"No clinical diagnosis language; validation warnings only", false),
			new Scenario("vegetarian-weekly-plan", "Plan semanal vegetariano de 7 días, lunes a viernes iguales.",
					Category.MENU_WEEKLY, Expectation.MUST_USE_TOOLS,
					List.of(SearchFoodCatalogToolService.TOOL_NAME, SearchDishCatalogToolService.TOOL_NAME,
							CreateDietPlanDraftToolService.TOOL_NAME),
					List.of("vegetariano", "supuesto", "Borrador IA"),
					"Weekly vegetarian structure with repetition note in assumptions", false),
			new Scenario("unsupported-ingredient", "Receta con pitahaya del catálogo.", Category.CATALOG_LOOKUP,
					Expectation.MUST_SEARCH_CATALOG, List.of(SearchFoodCatalogToolService.TOOL_NAME),
					List.of("catálogo", "no se encontró", "sustituto"),
					"Must not invent foods missing from catalog (#361)", false),
			new Scenario("menu-missing-calorie-target", "Arma un menú de un día con desayuno, comida y cena.",
					Category.CLARIFY, Expectation.MUST_CLARIFY, List.of(), List.of("objetivo calórico", "kcal"),
					"Ask for calorie target before extensive draft (#367)", false));

}
