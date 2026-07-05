package com.nutriconsultas.ai;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Dispatches OpenAI tool calls to registered nutrition tool services (#385).
 */
@Service
public class AiOrchestrationToolDispatcher {

	private final SearchFoodCatalogToolService searchFoodCatalogToolService;

	private final GetFoodNutrientsToolService getFoodNutrientsToolService;

	private final SearchDishCatalogToolService searchDishCatalogToolService;

	private final CalculateRecipeNutrientsToolService calculateRecipeNutrientsToolService;

	private final ValidatePlanConstraintsToolService validatePlanConstraintsToolService;

	private final CreateDishDraftToolService createDishDraftToolService;

	private final CreateMenuDraftToolService createMenuDraftToolService;

	private final CreateDietPlanDraftToolService createDietPlanDraftToolService;

	private final GetPatientAppointmentsToolService getPatientAppointmentsToolService;

	private final AiDraftToolSchemaValidator draftToolSchemaValidator;

	public AiOrchestrationToolDispatcher(final SearchFoodCatalogToolService searchFoodCatalogToolService,
			final GetFoodNutrientsToolService getFoodNutrientsToolService,
			final SearchDishCatalogToolService searchDishCatalogToolService,
			final CalculateRecipeNutrientsToolService calculateRecipeNutrientsToolService,
			final ValidatePlanConstraintsToolService validatePlanConstraintsToolService,
			final CreateDishDraftToolService createDishDraftToolService,
			final CreateMenuDraftToolService createMenuDraftToolService,
			final CreateDietPlanDraftToolService createDietPlanDraftToolService,
			final GetPatientAppointmentsToolService getPatientAppointmentsToolService,
			final AiDraftToolSchemaValidator draftToolSchemaValidator) {
		this.searchFoodCatalogToolService = searchFoodCatalogToolService;
		this.getFoodNutrientsToolService = getFoodNutrientsToolService;
		this.searchDishCatalogToolService = searchDishCatalogToolService;
		this.calculateRecipeNutrientsToolService = calculateRecipeNutrientsToolService;
		this.validatePlanConstraintsToolService = validatePlanConstraintsToolService;
		this.createDishDraftToolService = createDishDraftToolService;
		this.createMenuDraftToolService = createMenuDraftToolService;
		this.createDietPlanDraftToolService = createDietPlanDraftToolService;
		this.getPatientAppointmentsToolService = getPatientAppointmentsToolService;
		this.draftToolSchemaValidator = draftToolSchemaValidator;
	}

	public String dispatch(final AiOrchestrationContext context, final String toolName, final String argumentsJson) {
		final Object result = execute(context, toolName, argumentsJson);
		return AiToolJsonSerializer.toJson(result);
	}

	private Object execute(final AiOrchestrationContext context, final String toolName, final String argumentsJson) {
		if (SearchFoodCatalogToolService.TOOL_NAME.equals(toolName)) {
			return searchFoodCatalog(context, argumentsJson);
		}
		if (GetFoodNutrientsToolService.TOOL_NAME.equals(toolName)) {
			return getFoodNutrients(context, argumentsJson);
		}
		if (SearchDishCatalogToolService.TOOL_NAME.equals(toolName)) {
			return searchDishCatalog(context, argumentsJson);
		}
		if (CalculateRecipeNutrientsToolService.TOOL_NAME.equals(toolName)) {
			return calculateRecipeNutrients(context, argumentsJson);
		}
		if (ValidatePlanConstraintsToolService.TOOL_NAME.equals(toolName)) {
			return validatePlanConstraints(context, argumentsJson);
		}
		if (CreateDishDraftToolService.TOOL_NAME.equals(toolName)) {
			return createDishDraft(context, argumentsJson);
		}
		if (CreateMenuDraftToolService.TOOL_NAME.equals(toolName)) {
			return createMenuDraft(context, argumentsJson);
		}
		if (CreateDietPlanDraftToolService.TOOL_NAME.equals(toolName)) {
			return createDietPlanDraft(context, argumentsJson);
		}
		if (GetPatientAppointmentsToolService.TOOL_NAME.equals(toolName)) {
			return getPatientAppointments(context, argumentsJson);
		}
		return AiToolResult.error(AiToolErrorCode.VALIDATION, "Herramienta no reconocida: " + toolName);
	}

	private AiToolResult<FoodCatalogSearchData> searchFoodCatalog(final AiOrchestrationContext context,
			final String argumentsJson) {
		final JsonNode root = AiToolJsonSerializer.parseJson(argumentsJson);
		final String query = requiredText(root, "query");
		return searchFoodCatalogToolService.search(context.nutritionistId(), query, optionalText(root, "clasificacion"),
				optionalInteger(root, "limit"));
	}

	private AiToolResult<FoodNutrientsData> getFoodNutrients(final AiOrchestrationContext context,
			final String argumentsJson) {
		final JsonNode root = AiToolJsonSerializer.parseJson(argumentsJson);
		final long alimentoId = requiredLong(root, "alimentoId");
		final String cantidad = requiredText(root, "cantidad");
		return getFoodNutrientsToolService.getNutrients(context.nutritionistId(), alimentoId, cantidad,
				optionalInteger(root, "pesoNetoG"), optionalInteger(root, "portions"), optionalText(root, "unidad"));
	}

	private AiToolResult<DishCatalogSearchData> searchDishCatalog(final AiOrchestrationContext context,
			final String argumentsJson) {
		final JsonNode root = AiToolJsonSerializer.parseJson(argumentsJson);
		final String query = requiredText(root, "query");
		return searchDishCatalogToolService.search(context.nutritionistId(), query,
				optionalText(root, "ingestasSugeridas"), optionalInteger(root, "limit"));
	}

	private AiToolResult<RecipeNutrientsData> calculateRecipeNutrients(final AiOrchestrationContext context,
			final String argumentsJson) {
		final JsonNode root = AiToolJsonSerializer.parseJson(argumentsJson);
		final JsonNode ingredientsNode = root.get("ingredients");
		if (ingredientsNode == null || !ingredientsNode.isArray()) {
			throw new AiOrchestrationException("La lista de ingredientes es obligatoria.");
		}
		final List<RecipeIngredientInput> ingredients = AiToolJsonSerializer.convertList(ingredientsNode,
				RecipeIngredientInput.class);
		return calculateRecipeNutrientsToolService.calculate(context.nutritionistId(), ingredients,
				optionalInteger(root, "portions"), optionalText(root, "label"));
	}

	private AiToolResult<PlanConstraintValidationData> validatePlanConstraints(final AiOrchestrationContext context,
			final String argumentsJson) {
		final ValidatePlanConstraintsRequest request = AiToolJsonSerializer.fromJson(argumentsJson,
				ValidatePlanConstraintsRequest.class);
		return validatePlanConstraintsToolService.validate(context.nutritionistId(), request, context.patientContext());
	}

	private AiToolResult<AiDraftCreationData> createDishDraft(final AiOrchestrationContext context,
			final String argumentsJson) {
		final Optional<String> schemaViolation = draftToolSchemaValidator.validateDishDraftArguments(argumentsJson);
		if (schemaViolation.isPresent()) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, schemaViolation.get());
		}
		final DishDraftInput input = AiToolJsonSerializer.fromJson(argumentsJson, DishDraftInput.class);
		return createDishDraftToolService.createDraft(context.nutritionistId(), context.threadId(), input);
	}

	private AiToolResult<AiDraftCreationData> createMenuDraft(final AiOrchestrationContext context,
			final String argumentsJson) {
		final Optional<String> schemaViolation = draftToolSchemaValidator.validateMenuDraftArguments(argumentsJson);
		if (schemaViolation.isPresent()) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, schemaViolation.get());
		}
		final MenuDraftInput input = AiToolJsonSerializer.fromJson(argumentsJson, MenuDraftInput.class);
		return createMenuDraftToolService.createDraft(context.nutritionistId(), context.threadId(), input);
	}

	private AiToolResult<AiDraftCreationData> createDietPlanDraft(final AiOrchestrationContext context,
			final String argumentsJson) {
		final Optional<String> schemaViolation = draftToolSchemaValidator.validateDietPlanDraftArguments(argumentsJson);
		if (schemaViolation.isPresent()) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION, schemaViolation.get());
		}
		final DietPlanDraftInput input = AiToolJsonSerializer.fromJson(argumentsJson, DietPlanDraftInput.class);
		return createDietPlanDraftToolService.createDraft(context.nutritionistId(), context.threadId(), input);
	}

	private AiToolResult<PatientAppointmentsData> getPatientAppointments(final AiOrchestrationContext context,
			final String argumentsJson) {
		if (context.patientContext() == null || context.patientContext().patientId() == null) {
			return AiToolResult.error(AiToolErrorCode.VALIDATION,
					"Esta conversación no tiene un paciente vinculado. Abre el chat desde el expediente del paciente.");
		}
		final JsonNode root = AiToolJsonSerializer.parseJson(argumentsJson);
		final PatientAppointmentScope scope = parseAppointmentScope(root);
		return getPatientAppointmentsToolService.getAppointments(context.nutritionistId(),
				context.patientContext().patientId(), scope, optionalInteger(root, "limit"));
	}

	private static PatientAppointmentScope parseAppointmentScope(final JsonNode root) {
		final JsonNode scopeNode = root.get("scope");
		if (scopeNode == null || scopeNode.isNull() || !scopeNode.isTextual()) {
			return null;
		}
		final String value = scopeNode.asText();
		if (value.isBlank()) {
			return null;
		}
		try {
			return PatientAppointmentScope.valueOf(value);
		}
		catch (final IllegalArgumentException ex) {
			throw new AiOrchestrationException("El campo 'scope' debe ser UPCOMING, PAST o ALL.", ex);
		}
	}

	private static String requiredText(final JsonNode root, final String field) {
		final JsonNode node = root.get(field);
		if (node == null || node.isNull() || !node.isTextual() || node.asText().isBlank()) {
			throw new AiOrchestrationException("El campo '" + field + "' es obligatorio.");
		}
		return node.asText();
	}

	private static String optionalText(final JsonNode root, final String field) {
		final JsonNode node = root.get(field);
		if (node == null || node.isNull() || !node.isTextual()) {
			return null;
		}
		final String value = node.asText();
		return value.isBlank() ? null : value;
	}

	private static Integer optionalInteger(final JsonNode root, final String field) {
		final JsonNode node = root.get(field);
		if (node == null || node.isNull() || !node.isNumber()) {
			return null;
		}
		return node.intValue();
	}

	private static long requiredLong(final JsonNode root, final String field) {
		final JsonNode node = root.get(field);
		if (node == null || node.isNull() || !node.isNumber()) {
			throw new AiOrchestrationException("El campo '" + field + "' es obligatorio.");
		}
		return node.longValue();
	}

}
