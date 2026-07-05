package com.nutriconsultas.ai.mcp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.nutriconsultas.ai.AiOpenAiToolCatalog;
import com.nutriconsultas.ai.OpenAiToolDefinition;

/**
 * MCP tool descriptors derived from {@link AiOpenAiToolCatalog} (#393).
 */
@Component
public final class McpToolDescriptorCatalog {

	/**
	 * Descriptor set version — bump when MCP names or schemas change incompatibly.
	 */
	public static final String DESCRIPTOR_VERSION = "1.0.0";

	private final AiOpenAiToolCatalog openAiToolCatalog;

	public McpToolDescriptorCatalog(final AiOpenAiToolCatalog openAiToolCatalog) {
		this.openAiToolCatalog = openAiToolCatalog;
	}

	public String descriptorVersion() {
		return DESCRIPTOR_VERSION;
	}

	public List<McpToolDescriptor> descriptors() {
		final Map<String, OpenAiToolDefinition> byInternalName = openAiToolCatalog.definitions()
			.stream()
			.collect(Collectors.toUnmodifiableMap(OpenAiToolDefinition::name, Function.identity()));
		final List<McpToolDescriptor> result = new ArrayList<>();
		for (final McpToolRegistry entry : McpToolRegistry.orderedEntries()) {
			final OpenAiToolDefinition definition = byInternalName.get(entry.internalToolName());
			if (definition == null) {
				throw new IllegalStateException(
						"Missing OpenAI tool definition for MCP mapping: " + entry.internalToolName());
			}
			result.add(entry.toDescriptor(definition));
		}
		return List.copyOf(result);
	}

	public Optional<String> internalToolNameFor(final String mcpToolName) {
		return McpToolRegistry.findByMcpName(mcpToolName).map(McpToolRegistry::internalToolName);
	}

	public Optional<String> mcpToolNameFor(final String internalToolName) {
		return McpToolRegistry.findByInternalName(internalToolName).map(McpToolRegistry::mcpName);
	}

	public boolean requiresThreadId(final String mcpToolName) {
		return McpToolRegistry.findByMcpName(mcpToolName).map(McpToolRegistry::requiresThreadId).orElse(false);
	}

}
