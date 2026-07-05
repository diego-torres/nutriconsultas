package com.nutriconsultas.ai.mcp;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.nutriconsultas.ai.CalculateRecipeNutrientsToolService;
import com.nutriconsultas.ai.CreateDietPlanDraftToolService;
import com.nutriconsultas.ai.CreateDishDraftToolService;
import com.nutriconsultas.ai.CreateMenuDraftToolService;
import com.nutriconsultas.ai.GetFoodNutrientsToolService;
import com.nutriconsultas.ai.OpenAiToolDefinition;
import com.nutriconsultas.ai.SearchDishCatalogToolService;
import com.nutriconsultas.ai.SearchFoodCatalogToolService;
import com.nutriconsultas.ai.ValidatePlanConstraintsToolService;

/**
 * Stable MCP tool names and metadata (#393). Versioned as a set — add new enum constants
 * for v2 tools.
 */
enum McpToolRegistry {

	SEARCH_FOODS("catalog.search_foods", SearchFoodCatalogToolService.TOOL_NAME, "Buscar alimentos",
			"Busca alimentos en el catálogo autorizado.", true, false),

	GET_FOOD_NUTRIENTS("catalog.get_food_nutrients", GetFoodNutrientsToolService.TOOL_NAME, "Nutrientes de alimento",
			"Obtiene nutrientes de un alimento del catálogo para una cantidad dada.", true, false),

	SEARCH_DISHES("catalog.search_dishes", SearchDishCatalogToolService.TOOL_NAME, "Buscar platillos",
			"Busca platillos del catálogo del sistema y del nutriólogo autenticado.", true, false),

	CALCULATE_RECIPE("nutrition.calculate_recipe", CalculateRecipeNutrientsToolService.TOOL_NAME,
			"Calcular nutrientes de receta",
			"Calcula nutrientes totales y por porción de una lista de ingredientes del catálogo.", true, false),

	VALIDATE_PLAN("nutrition.validate_plan", ValidatePlanConstraintsToolService.TOOL_NAME, "Validar plan nutricional",
			"Valida un borrador de menú o plan contra objetivos calóricos, macros y restricciones.", true, false),

	CREATE_DISH("draft.create_dish", CreateDishDraftToolService.TOOL_NAME, "Crear borrador de platillo",
			"Guarda un borrador de platillo para revisión del nutriólogo. No guarda en el catálogo final.", false,
			true),

	CREATE_MENU("draft.create_menu", CreateMenuDraftToolService.TOOL_NAME, "Crear borrador de menú",
			"Guarda un borrador de menú de un día. No asigna al paciente.", false, true),

	CREATE_DIET_PLAN("draft.create_diet_plan", CreateDietPlanDraftToolService.TOOL_NAME,
			"Crear borrador de plan alimentario",
			"Guarda un borrador de plan alimenticio multi-día. No asigna al paciente.", false, true);

	private static final Map<String, McpToolRegistry> BY_MCP_NAME = Arrays.stream(values())
		.collect(Collectors.toUnmodifiableMap(McpToolRegistry::mcpName, Function.identity()));

	private static final Map<String, McpToolRegistry> BY_INTERNAL_NAME = Arrays.stream(values())
		.collect(Collectors.toUnmodifiableMap(McpToolRegistry::internalToolName, Function.identity()));

	private final String registeredMcpName;

	private final String registeredInternalToolName;

	private final String title;

	private final String summaryDescription;

	private final boolean readOnly;

	private final boolean requiresNutritionistConfirmation;

	McpToolRegistry(final String mcpName, final String internalToolName, final String title,
			final String summaryDescription, final boolean readOnly, final boolean requiresNutritionistConfirmation) {
		this.registeredMcpName = mcpName;
		this.registeredInternalToolName = internalToolName;
		this.title = title;
		this.summaryDescription = summaryDescription;
		this.readOnly = readOnly;
		this.requiresNutritionistConfirmation = requiresNutritionistConfirmation;
	}

	String mcpName() {
		return registeredMcpName;
	}

	String internalToolName() {
		return registeredInternalToolName;
	}

	boolean requiresThreadId() {
		return requiresNutritionistConfirmation;
	}

	static Optional<McpToolRegistry> findByMcpName(final String mcpName) {
		return Optional.ofNullable(BY_MCP_NAME.get(mcpName));
	}

	static Optional<McpToolRegistry> findByInternalName(final String internalToolName) {
		return Optional.ofNullable(BY_INTERNAL_NAME.get(internalToolName));
	}

	static McpToolRegistry[] orderedEntries() {
		return values();
	}

	McpToolDescriptor toDescriptor(final OpenAiToolDefinition openAiDefinition) {
		final String description = buildDescription(openAiDefinition.description());
		return new McpToolDescriptor(registeredMcpName, title, description, openAiDefinition.parameters(), readOnly,
				requiresNutritionistConfirmation);
	}

	private String buildDescription(final String openAiDescription) {
		if (requiresNutritionistConfirmation) {
			return summaryDescription + " "
					+ "Requiere revisión y aprobación del nutriólogo en Minutriporcion antes de guardar en el catálogo o asignar a un paciente. "
					+ openAiDescription;
		}
		return summaryDescription + " " + openAiDescription;
	}

}
