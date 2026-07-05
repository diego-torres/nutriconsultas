package com.nutriconsultas.ai.mcp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.nutriconsultas.ai.AiAuditLogger;
import com.nutriconsultas.ai.AiChatException;
import com.nutriconsultas.ai.AiChatPersistence;
import com.nutriconsultas.ai.AiChatPromptContext;
import com.nutriconsultas.ai.AiChatPromptContextResolvers;
import com.nutriconsultas.ai.AiChatRequestGuards;
import com.nutriconsultas.ai.AiChatThread;
import com.nutriconsultas.ai.AiErrorMessages;
import com.nutriconsultas.ai.AiOrchestrationContext;
import com.nutriconsultas.ai.AiOrchestrationException;
import com.nutriconsultas.ai.AiOrchestrationGuardrails;
import com.nutriconsultas.ai.AiOrchestrationToolDispatcher;
import com.nutriconsultas.ai.AiProperties;
import com.nutriconsultas.ai.AiToolErrorCode;
import com.nutriconsultas.ai.AiToolJsonSerializer;
import com.nutriconsultas.ai.AiToolResult;

/**
 * MCP JSON-RPC dispatch to {@link AiOrchestrationToolDispatcher} (#394).
 */
@Service
public final class McpToolDispatchService {

	private static final String DRAFT_THREAD_REQUIRED = "Se requiere una conversación activa para crear borradores.";

	private final AiProperties aiProperties;

	private final AiChatRequestGuards chatRequestGuards;

	private final McpToolDescriptorCatalog descriptorCatalog;

	private final AiOrchestrationToolDispatcher toolDispatcher;

	private final AiOrchestrationGuardrails guardrails;

	private final AiChatPromptContextResolvers contextResolvers;

	private final AiChatPersistence chatPersistence;

	private final AiAuditLogger auditLogger;

	public McpToolDispatchService(final AiProperties aiProperties, final AiChatRequestGuards chatRequestGuards,
			final McpToolDescriptorCatalog descriptorCatalog, final AiOrchestrationToolDispatcher toolDispatcher,
			final AiOrchestrationGuardrails guardrails, final AiChatPromptContextResolvers contextResolvers,
			final AiChatPersistence chatPersistence, final AiAuditLogger auditLogger) {
		this.aiProperties = aiProperties;
		this.chatRequestGuards = chatRequestGuards;
		this.descriptorCatalog = descriptorCatalog;
		this.toolDispatcher = toolDispatcher;
		this.guardrails = guardrails;
		this.contextResolvers = contextResolvers;
		this.chatPersistence = chatPersistence;
		this.auditLogger = auditLogger;
	}

	public Map<String, Object> handle(final String nutritionistId, final Map<String, Object> requestBody) {
		assertFeatureAccess(nutritionistId);
		final JsonNode root = toJsonNode(requestBody);
		final Object id = parseId(root);
		if (!"2.0".equals(root.path("jsonrpc").asText())) {
			return McpJsonRpcResponses.error(id, McpJsonRpcResponses.ERROR_INVALID_REQUEST,
					"Solicitud JSON-RPC no válida.");
		}
		final String method = root.path("method").asText(null);
		if (!StringUtils.hasText(method)) {
			return McpJsonRpcResponses.error(id, McpJsonRpcResponses.ERROR_INVALID_REQUEST,
					"Solicitud JSON-RPC no válida.");
		}
		return switch (method) {
			case "initialize" -> handleInitialize(id, root.path("params"));
			case "tools/list" -> handleToolsList(id);
			case "tools/call" -> handleToolsCall(nutritionistId, id, root.path("params"));
			case "ping" -> McpJsonRpcResponses.success(id, Map.of());
			default ->
				McpJsonRpcResponses.error(id, McpJsonRpcResponses.ERROR_METHOD_NOT_FOUND, "Método MCP no reconocido.");
		};
	}

	private Map<String, Object> handleInitialize(final Object id, final JsonNode params) {
		final Map<String, Object> result = new LinkedHashMap<>();
		result.put("protocolVersion", McpJsonRpcResponses.PROTOCOL_VERSION);
		result.put("capabilities", Map.of("tools", Map.of("listChanged", false)));
		result.put("serverInfo",
				Map.of("name", "nutriconsultas", "version", McpToolDescriptorCatalog.DESCRIPTOR_VERSION));
		if (params != null && params.has("_meta")) {
			result.put("_meta", params.get("_meta"));
		}
		return McpJsonRpcResponses.success(id, result);
	}

	private Map<String, Object> handleToolsList(final Object id) {
		final List<Map<String, Object>> tools = new ArrayList<>();
		for (final McpToolDescriptor descriptor : descriptorCatalog.descriptors()) {
			tools.add(descriptor.toMap());
		}
		return McpJsonRpcResponses.success(id, Map.of("tools", tools));
	}

	private Map<String, Object> handleToolsCall(final String nutritionistId, final Object id, final JsonNode params) {
		final String mcpToolName = params.path("name").asText(null);
		if (!StringUtils.hasText(mcpToolName)) {
			return toolErrorResponse(id,
					AiToolResult.error(AiToolErrorCode.VALIDATION, "Nombre de herramienta requerido."));
		}
		final Optional<String> internalToolName = descriptorCatalog.internalToolNameFor(mcpToolName);
		if (internalToolName.isEmpty()) {
			return toolErrorResponse(id,
					AiToolResult.error(AiToolErrorCode.VALIDATION, "Herramienta MCP no reconocida: " + mcpToolName));
		}
		final Long threadId = extractThreadId(params);
		if (descriptorCatalog.requiresThreadId(mcpToolName) && threadId == null) {
			return toolErrorResponse(id, AiToolResult.error(AiToolErrorCode.VALIDATION, DRAFT_THREAD_REQUIRED));
		}
		final AiOrchestrationContext context = buildContext(nutritionistId, threadId);
		final String openAiToolName = internalToolName.get();
		if (!guardrails.isToolAllowed(openAiToolName)) {
			auditLogger.logToolRejected(context.threadId(), openAiToolName);
			return toolErrorResponse(id, AiToolResult.error(AiToolErrorCode.VALIDATION, "Herramienta no permitida."));
		}
		final String argumentsJson = serializeArguments(params.path("arguments"));
		try {
			final String rawResult = toolDispatcher.dispatch(context, openAiToolName, argumentsJson);
			final String sanitized = guardrails.sanitizeToolResult(openAiToolName, rawResult);
			final boolean success = isSuccessfulToolResult(sanitized);
			auditLogger.logMcpToolCall(context.threadId(), nutritionistId, mcpToolName, openAiToolName, success);
			return McpJsonRpcResponses.toolCallResult(id, sanitized, !success);
		}
		catch (final AiOrchestrationException ex) {
			auditLogger.logToolDispatchFailed(context.threadId(), openAiToolName);
			return toolErrorResponse(id, AiToolResult.error(AiToolErrorCode.VALIDATION, ex.getMessage()));
		}
	}

	private void assertFeatureAccess(final String nutritionistId) {
		if (!aiProperties.isEnabled()) {
			if (aiProperties.isEnabledButMisconfigured()) {
				throw new McpAccessException(HttpStatus.SERVICE_UNAVAILABLE, AiErrorMessages.MISCONFIGURATION);
			}
			throw new McpAccessException(HttpStatus.SERVICE_UNAVAILABLE, "El asistente de IA no está habilitado.");
		}
		try {
			chatRequestGuards.assertNutritionistAccess(nutritionistId);
		}
		catch (final AiChatException ex) {
			throw new McpAccessException(ex.getHttpStatus(), ex.getMessage(), ex);
		}
	}

	private AiOrchestrationContext buildContext(final String nutritionistId, @Nullable final Long threadId) {
		if (threadId == null) {
			return new AiOrchestrationContext(nutritionistId, 0L, null, null, null);
		}
		final AiChatThread thread = chatPersistence.getThreadRepository()
			.findByIdAndNutritionistId(threadId, nutritionistId)
			.orElseThrow(() -> new McpAccessException(HttpStatus.NOT_FOUND, "No se encontró la conversación."));
		final Long patientId = thread.getPatient() != null ? thread.getPatient().getId() : null;
		final AiChatPromptContext promptContext = new AiChatPromptContext(patientId, null, null);
		return contextResolvers.buildOrchestrationContext(nutritionistId, threadId, promptContext);
	}

	private static Map<String, Object> toolErrorResponse(final Object id, final AiToolResult<?> toolResult) {
		return McpJsonRpcResponses.toolCallResult(id, AiToolJsonSerializer.toJson(toolResult), true);
	}

	private static JsonNode toJsonNode(final Map<String, Object> requestBody) {
		return AiToolJsonSerializer.parseJson(AiToolJsonSerializer.toJson(requestBody));
	}

	private static Object parseId(final JsonNode root) {
		final JsonNode idNode = root.get("id");
		if (idNode == null || idNode.isNull()) {
			return null;
		}
		if (idNode.isNumber()) {
			return idNode.numberValue();
		}
		return idNode.asText();
	}

	@Nullable
	private static Long extractThreadId(final JsonNode params) {
		final JsonNode meta = params.path("_meta");
		if (meta.isMissingNode() || meta.isNull()) {
			return null;
		}
		final JsonNode threadIdNode = meta.path("threadId");
		if (threadIdNode.isMissingNode() || threadIdNode.isNull() || !threadIdNode.isNumber()) {
			return null;
		}
		return threadIdNode.longValue();
	}

	private static String serializeArguments(final JsonNode argumentsNode) {
		if (argumentsNode == null || argumentsNode.isMissingNode() || argumentsNode.isNull()) {
			return "{}";
		}
		return AiToolJsonSerializer.toJson(argumentsNode);
	}

	private static boolean isSuccessfulToolResult(final String json) {
		final JsonNode node = AiToolJsonSerializer.parseJson(json);
		return !node.has("success") || node.get("success").asBoolean(true);
	}

}
