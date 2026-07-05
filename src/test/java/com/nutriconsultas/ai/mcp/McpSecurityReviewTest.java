package com.nutriconsultas.ai.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.nutriconsultas.ai.AiOpenAiToolCatalog;
import com.nutriconsultas.ai.AiToolAllowlist;
import com.nutriconsultas.ai.CreateDishDraftToolService;
import com.nutriconsultas.ai.CreateDietPlanDraftToolService;
import com.nutriconsultas.ai.CreateMenuDraftToolService;

/**
 * MCP tool surface security assertions (#395).
 */
class McpSecurityReviewTest {

	private static final Set<String> FORBIDDEN_MCP_NAME_FRAGMENTS = Set.of("accept", "delete", "assign", "save",
			"update", "remove", "discard", "materialize");

	private static final Set<String> FORBIDDEN_INTERNAL_TOOL_FRAGMENTS = Set.of("accept", "delete", "assign",
			"materialize", "discard");

	private final McpToolDescriptorCatalog catalog = new McpToolDescriptorCatalog(new AiOpenAiToolCatalog());

	private final AiToolAllowlist allowlist = new AiToolAllowlist(new AiOpenAiToolCatalog());

	@Test
	void mcpExposesOnlyAllowlistedInternalTools() {
		final Set<String> mcpInternalNames = catalog.descriptors()
			.stream()
			.map(descriptor -> catalog.internalToolNameFor(descriptor.name()).orElseThrow())
			.collect(Collectors.toSet());

		assertThat(mcpInternalNames).isEqualTo(allowlist.allowedToolNames());
		assertThat(mcpInternalNames).hasSize(8);
	}

	@Test
	void mcpDoesNotExposeDestructiveToolNamePatterns() {
		for (final McpToolDescriptor descriptor : catalog.descriptors()) {
			final String lowerName = descriptor.name().toLowerCase();
			assertThat(FORBIDDEN_MCP_NAME_FRAGMENTS).noneMatch(lowerName::contains);

			final String internalName = catalog.internalToolNameFor(descriptor.name()).orElseThrow().toLowerCase();
			assertThat(FORBIDDEN_INTERNAL_TOOL_FRAGMENTS).noneMatch(internalName::contains);
		}
	}

	@Test
	void writeActionsAreDraftToolsRequiringConfirmationAndThread() {
		final List<McpToolDescriptor> writeTools = catalog.descriptors()
			.stream()
			.filter(descriptor -> !descriptor.readOnly())
			.toList();

		assertThat(writeTools).hasSize(3);
		assertThat(writeTools.stream().map(McpToolDescriptor::name).collect(Collectors.toSet()))
			.containsExactlyInAnyOrder("draft.create_dish", "draft.create_menu", "draft.create_diet_plan");

		for (final McpToolDescriptor descriptor : writeTools) {
			assertThat(descriptor.requiresNutritionistConfirmation()).isTrue();
			final String description = descriptor.description();
			assertThat(description.contains("No guarda en el catálogo final")
					|| description.contains("No asigna al paciente"))
				.isTrue();
			assertThat(catalog.requiresThreadId(descriptor.name())).isTrue();
		}
	}

	@Test
	void readOnlyToolsDoNotRequireThreadOrConfirmation() {
		for (final McpToolDescriptor descriptor : catalog.descriptors()) {
			if (descriptor.readOnly()) {
				assertThat(descriptor.requiresNutritionistConfirmation()).isFalse();
				assertThat(catalog.requiresThreadId(descriptor.name())).isFalse();
			}
		}
	}

	@Test
	void draftInternalToolsMapToDraftServicesOnly() {
		assertThat(catalog.internalToolNameFor("draft.create_dish")).contains(CreateDishDraftToolService.TOOL_NAME);
		assertThat(catalog.internalToolNameFor("draft.create_menu")).contains(CreateMenuDraftToolService.TOOL_NAME);
		assertThat(catalog.internalToolNameFor("draft.create_diet_plan"))
			.contains(CreateDietPlanDraftToolService.TOOL_NAME);
	}

}
