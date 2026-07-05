package com.nutriconsultas.ai.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.nutriconsultas.ai.AiOpenAiToolCatalog;
import com.nutriconsultas.ai.CalculateRecipeNutrientsToolService;
import com.nutriconsultas.ai.CreateDietPlanDraftToolService;
import com.nutriconsultas.ai.CreateDishDraftToolService;
import com.nutriconsultas.ai.CreateMenuDraftToolService;
import com.nutriconsultas.ai.GetFoodNutrientsToolService;
import com.nutriconsultas.ai.OpenAiToolDefinition;
import com.nutriconsultas.ai.SearchDishCatalogToolService;
import com.nutriconsultas.ai.SearchFoodCatalogToolService;
import com.nutriconsultas.ai.ValidatePlanConstraintsToolService;

class McpToolDescriptorCatalogTest {

	private static final Set<String> EXPECTED_MCP_NAMES = Set.of("catalog.search_foods", "catalog.get_food_nutrients",
			"catalog.search_dishes", "nutrition.calculate_recipe", "nutrition.validate_plan", "draft.create_dish",
			"draft.create_menu", "draft.create_diet_plan", "calendar.get_patient_appointments");

	private final McpToolDescriptorCatalog catalog = new McpToolDescriptorCatalog(new AiOpenAiToolCatalog());

	@Test
	void descriptorsExposeEightStableMcpTools() {
		final List<McpToolDescriptor> descriptors = catalog.descriptors();

		assertThat(descriptors).hasSize(9);
		assertThat(descriptors.stream().map(McpToolDescriptor::name).collect(Collectors.toSet()))
			.isEqualTo(EXPECTED_MCP_NAMES);
		assertThat(catalog.descriptorVersion()).isEqualTo(McpToolDescriptorCatalog.DESCRIPTOR_VERSION);
	}

	@Test
	void eachDescriptorHasNameTitleDescriptionAndInputSchema() {
		for (final McpToolDescriptor descriptor : catalog.descriptors()) {
			assertThat(descriptor.name()).isNotBlank();
			assertThat(descriptor.title()).isNotBlank();
			assertThat(descriptor.description()).isNotBlank();
			assertThat(descriptor.inputSchema()).containsKey("type").containsEntry("type", "object");
			assertThat(descriptor.inputSchema()).containsKeys("properties", "required", "additionalProperties");

			final Map<String, Object> exported = descriptor.toMap();
			assertThat(exported).containsEntry("name", descriptor.name())
				.containsEntry("title", descriptor.title())
				.containsEntry("description", descriptor.description())
				.containsEntry("inputSchema", descriptor.inputSchema());
		}
	}

	@Test
	void readOnlyToolsAreMarkedReadOnly() {
		final Map<String, McpToolDescriptor> byName = catalog.descriptors()
			.stream()
			.collect(Collectors.toMap(McpToolDescriptor::name, descriptor -> descriptor));

		assertThat(byName.get("catalog.search_foods").readOnly()).isTrue();
		assertThat(byName.get("catalog.get_food_nutrients").readOnly()).isTrue();
		assertThat(byName.get("catalog.search_dishes").readOnly()).isTrue();
		assertThat(byName.get("nutrition.calculate_recipe").readOnly()).isTrue();
		assertThat(byName.get("nutrition.validate_plan").readOnly()).isTrue();
		assertThat(byName.get("calendar.get_patient_appointments").readOnly()).isTrue();

		for (final String readOnlyName : List.of("catalog.search_foods", "catalog.get_food_nutrients",
				"catalog.search_dishes", "nutrition.calculate_recipe", "nutrition.validate_plan",
				"calendar.get_patient_appointments")) {
			assertThat(byName.get(readOnlyName).toMap()).containsEntry("annotations", Map.of("readOnlyHint", true));
			assertThat(byName.get(readOnlyName).requiresNutritionistConfirmation()).isFalse();
		}
	}

	@Test
	void draftToolsRequireNutritionistConfirmation() {
		final Map<String, McpToolDescriptor> byName = catalog.descriptors()
			.stream()
			.collect(Collectors.toMap(McpToolDescriptor::name, descriptor -> descriptor));

		for (final String draftName : List.of("draft.create_dish", "draft.create_menu", "draft.create_diet_plan")) {
			final McpToolDescriptor descriptor = byName.get(draftName);
			assertThat(descriptor.readOnly()).isFalse();
			assertThat(descriptor.requiresNutritionistConfirmation()).isTrue();
			assertThat(descriptor.toMap()).containsEntry("requiresNutritionistConfirmation", true);
			assertThat(descriptor.description())
				.contains("Requiere revisión y aprobación del nutriólogo en Minutriporcion");
		}
	}

	@Test
	void inputSchemasMatchOpenAiToolCatalog() {
		final Map<String, OpenAiToolDefinition> openAiByName = new AiOpenAiToolCatalog().definitions()
			.stream()
			.collect(Collectors.toMap(OpenAiToolDefinition::name, definition -> definition));

		for (final McpToolDescriptor descriptor : catalog.descriptors()) {
			final String internalName = catalog.internalToolNameFor(descriptor.name()).orElseThrow();
			assertThat(descriptor.inputSchema()).isEqualTo(openAiByName.get(internalName).parameters());
		}
	}

	@Test
	void mcpAndInternalNameMappingsAreBidirectional() {
		assertThat(catalog.internalToolNameFor("catalog.search_foods"))
			.contains(SearchFoodCatalogToolService.TOOL_NAME);
		assertThat(catalog.internalToolNameFor("nutrition.validate_plan"))
			.contains(ValidatePlanConstraintsToolService.TOOL_NAME);
		assertThat(catalog.mcpToolNameFor(CreateMenuDraftToolService.TOOL_NAME)).contains("draft.create_menu");
		assertThat(catalog.mcpToolNameFor(GetFoodNutrientsToolService.TOOL_NAME))
			.contains("catalog.get_food_nutrients");
		assertThat(catalog.mcpToolNameFor(CalculateRecipeNutrientsToolService.TOOL_NAME))
			.contains("nutrition.calculate_recipe");
		assertThat(catalog.mcpToolNameFor(CreateDietPlanDraftToolService.TOOL_NAME)).contains("draft.create_diet_plan");
		assertThat(catalog.mcpToolNameFor(CreateDishDraftToolService.TOOL_NAME)).contains("draft.create_dish");
		assertThat(catalog.mcpToolNameFor(SearchDishCatalogToolService.TOOL_NAME)).contains("catalog.search_dishes");
	}

	@Test
	void draftToolsRequireThreadId() {
		assertThat(catalog.requiresThreadId("draft.create_dish")).isTrue();
		assertThat(catalog.requiresThreadId("catalog.search_foods")).isFalse();
	}

}
