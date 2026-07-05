package com.nutriconsultas.ai.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionTemplate;

import com.nutriconsultas.ai.AiAuditLogger;
import com.nutriconsultas.ai.AiChatException;
import com.nutriconsultas.ai.AiChatMessageRepository;
import com.nutriconsultas.ai.AiChatPersistence;
import com.nutriconsultas.ai.AiChatPromptContextResolvers;
import com.nutriconsultas.ai.AiChatRequestGuards;
import com.nutriconsultas.ai.AiChatThread;
import com.nutriconsultas.ai.AiChatThreadRepository;
import com.nutriconsultas.ai.AiOpenAiToolCatalog;
import com.nutriconsultas.ai.AiOrchestrationContext;
import com.nutriconsultas.ai.AiOrchestrationGuardrails;
import com.nutriconsultas.ai.AiOrchestrationToolDispatcher;
import com.nutriconsultas.ai.AiProperties;
import com.nutriconsultas.ai.AiToolErrorCode;
import com.nutriconsultas.ai.SearchFoodCatalogToolService;
import com.nutriconsultas.subscription.SubscriptionErrorResponses;
import com.nutriconsultas.subscription.SubscriptionLimitExceededException;

@ExtendWith(MockitoExtension.class)
class McpToolDispatchServiceTest {

	private static final String NUTRITIONIST_ID = "auth0|nutritionist-a";

	@Mock
	private AiProperties aiProperties;

	@Mock
	private AiChatRequestGuards chatRequestGuards;

	@Mock
	private AiOrchestrationToolDispatcher toolDispatcher;

	@Mock
	private AiOrchestrationGuardrails guardrails;

	@Mock
	private AiChatPromptContextResolvers contextResolvers;

	@Mock
	private AiChatMessageRepository messageRepository;

	@Mock
	private TransactionTemplate transactionTemplate;

	@Mock
	private AiAuditLogger auditLogger;

	@Mock
	private SubscriptionErrorResponses subscriptionErrorResponses;

	@Mock
	private AiChatThreadRepository threadRepository;

	private AiChatPersistence chatPersistence;

	private McpToolDescriptorCatalog descriptorCatalog;

	private McpToolDispatchService dispatchService;

	@BeforeEach
	void setUp() {
		chatPersistence = new AiChatPersistence(threadRepository, messageRepository, transactionTemplate);
		descriptorCatalog = new McpToolDescriptorCatalog(new AiOpenAiToolCatalog());
		dispatchService = new McpToolDispatchService(aiProperties, chatRequestGuards, descriptorCatalog, toolDispatcher,
				guardrails, contextResolvers, chatPersistence, auditLogger, subscriptionErrorResponses);
		when(aiProperties.isEnabled()).thenReturn(true);
	}

	@Test
	void toolsListReturnsEightDescriptors() {
		final Map<String, Object> response = dispatchService.handle(NUTRITIONIST_ID, jsonRpc("tools/list", Map.of()));

		assertThat(response).containsEntry("jsonrpc", "2.0");
		@SuppressWarnings("unchecked")
		final Map<String, Object> result = (Map<String, Object>) response.get("result");
		@SuppressWarnings("unchecked")
		final List<Map<String, Object>> tools = (List<Map<String, Object>>) result.get("tools");
		assertThat(tools).hasSize(9);
		verify(chatRequestGuards).assertNutritionistAccess(NUTRITIONIST_ID);
	}

	@Test
	void toolsCallDispatchesMappedInternalTool() {
		when(guardrails.isToolAllowed(SearchFoodCatalogToolService.TOOL_NAME)).thenReturn(true);
		when(toolDispatcher.dispatch(any(AiOrchestrationContext.class), eq(SearchFoodCatalogToolService.TOOL_NAME),
				any()))
			.thenReturn("{\"success\":true,\"data\":{\"items\":[],\"totalReturned\":0}}");
		when(guardrails.sanitizeToolResult(eq(SearchFoodCatalogToolService.TOOL_NAME), any()))
			.thenAnswer(invocation -> invocation.getArgument(1));

		final Map<String, Object> params = new LinkedHashMap<>();
		params.put("name", "catalog.search_foods");
		params.put("arguments", Map.of("query", "avena"));
		final Map<String, Object> response = dispatchService.handle(NUTRITIONIST_ID, jsonRpc("tools/call", params));

		@SuppressWarnings("unchecked")
		final Map<String, Object> result = (Map<String, Object>) response.get("result");
		assertThat(result.get("isError")).isEqualTo(false);
		verify(toolDispatcher).dispatch(any(AiOrchestrationContext.class), eq(SearchFoodCatalogToolService.TOOL_NAME),
				any());
		verify(auditLogger).logMcpToolCall(eq(0L), eq(NUTRITIONIST_ID), eq("catalog.search_foods"),
				eq(SearchFoodCatalogToolService.TOOL_NAME), eq(true));
	}

	@Test
	void toolsCallRejectsUnknownMcpTool() {
		final Map<String, Object> params = Map.of("name", "catalog.unknown", "arguments", Map.of());
		final Map<String, Object> response = dispatchService.handle(NUTRITIONIST_ID, jsonRpc("tools/call", params));

		@SuppressWarnings("unchecked")
		final Map<String, Object> result = (Map<String, Object>) response.get("result");
		assertThat(result.get("isError")).isEqualTo(true);
	}

	@Test
	void draftToolRequiresThreadId() {
		final Map<String, Object> params = Map.of("name", "draft.create_dish", "arguments",
				Map.of("name", "Ensalada", "ingredients", List.of(Map.of("alimentoId", 1, "cantidad", "1"))));
		final Map<String, Object> response = dispatchService.handle(NUTRITIONIST_ID, jsonRpc("tools/call", params));

		@SuppressWarnings("unchecked")
		final Map<String, Object> result = (Map<String, Object>) response.get("result");
		assertThat(result.get("isError")).isEqualTo(true);
	}

	@Test
	void rejectsWhenAiDisabled() {
		when(aiProperties.isEnabled()).thenReturn(false);
		when(aiProperties.isEnabledButMisconfigured()).thenReturn(false);

		org.assertj.core.api.Assertions
			.assertThatThrownBy(() -> dispatchService.handle(NUTRITIONIST_ID, jsonRpc("tools/list", Map.of())))
			.isInstanceOf(McpAccessException.class)
			.extracting(ex -> ((McpAccessException) ex).getStatus())
			.isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
	}

	@Test
	void propagatesEntitlementFailure() {
		org.mockito.Mockito.doThrow(new AiChatException(HttpStatus.FORBIDDEN, AiToolErrorCode.FORBIDDEN, "Sin acceso"))
			.when(chatRequestGuards)
			.assertNutritionistAccess(NUTRITIONIST_ID);

		org.assertj.core.api.Assertions
			.assertThatThrownBy(() -> dispatchService.handle(NUTRITIONIST_ID, jsonRpc("tools/list", Map.of())))
			.isInstanceOf(McpAccessException.class)
			.extracting(ex -> ((McpAccessException) ex).getStatus())
			.isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void propagatesSubscriptionEntitlementFailure() {
		final SubscriptionLimitExceededException denied = new SubscriptionLimitExceededException(
				SubscriptionErrorResponses.KEY_AI_ASSISTANT_DENIED);
		org.mockito.Mockito.doThrow(denied).when(chatRequestGuards).assertNutritionistAccess(NUTRITIONIST_ID);
		when(subscriptionErrorResponses.resolve(denied)).thenReturn("Plan Plus requerido.");

		org.assertj.core.api.Assertions
			.assertThatThrownBy(() -> dispatchService.handle(NUTRITIONIST_ID, jsonRpc("tools/list", Map.of())))
			.isInstanceOf(McpAccessException.class)
			.extracting(ex -> ((McpAccessException) ex).getStatus(), ex -> ((McpAccessException) ex).getUserMessage())
			.containsExactly(HttpStatus.FORBIDDEN, "Plan Plus requerido.");
	}

	@Test
	void crossTenantThreadIdReturnsNotFound() {
		when(threadRepository.findByIdAndNutritionistId(42L, NUTRITIONIST_ID)).thenReturn(Optional.empty());

		final Map<String, Object> params = new LinkedHashMap<>();
		params.put("name", "catalog.search_foods");
		params.put("arguments", Map.of("query", "avena"));
		params.put("_meta", Map.of("threadId", 42));

		org.assertj.core.api.Assertions
			.assertThatThrownBy(() -> dispatchService.handle(NUTRITIONIST_ID, jsonRpc("tools/call", params)))
			.isInstanceOf(McpAccessException.class)
			.extracting(ex -> ((McpAccessException) ex).getStatus())
			.isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void toolsCallUsesThreadContextWhenMetaProvided() {
		final AiChatThread thread = new AiChatThread();
		thread.setId(9L);
		thread.setNutritionistId(NUTRITIONIST_ID);
		when(threadRepository.findByIdAndNutritionistId(9L, NUTRITIONIST_ID)).thenReturn(Optional.of(thread));
		when(contextResolvers.buildOrchestrationContext(eq(NUTRITIONIST_ID), eq(9L), any()))
			.thenReturn(new AiOrchestrationContext(NUTRITIONIST_ID, 9L, null, null, null));
		when(guardrails.isToolAllowed(SearchFoodCatalogToolService.TOOL_NAME)).thenReturn(true);
		when(toolDispatcher.dispatch(any(), any(), any())).thenReturn("{\"success\":true,\"data\":{}}");
		when(guardrails.sanitizeToolResult(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));

		final Map<String, Object> params = new LinkedHashMap<>();
		params.put("name", "catalog.search_foods");
		params.put("arguments", Map.of("query", "avena"));
		params.put("_meta", Map.of("threadId", 9));
		dispatchService.handle(NUTRITIONIST_ID, jsonRpc("tools/call", params));

		verify(contextResolvers).buildOrchestrationContext(eq(NUTRITIONIST_ID), eq(9L), any());
	}

	private static Map<String, Object> jsonRpc(final String method, final Map<String, Object> params) {
		final Map<String, Object> body = new LinkedHashMap<>();
		body.put("jsonrpc", "2.0");
		body.put("id", 1);
		body.put("method", method);
		body.put("params", params);
		return body;
	}

}
