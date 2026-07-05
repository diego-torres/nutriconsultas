package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Golden prompt evaluation for supported nutrition workflows (#401). Uses mocked OpenAI
 * only — no live API calls.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiNutritionGoldenPromptTest {

	private static final String NUTRITIONIST_ID = "auth0|nutritionist-a";

	private static final long THREAD_ID = 10L;

	@InjectMocks
	private AiOrchestrationServiceImpl service;

	@Mock
	private AiProperties properties;

	@Mock
	private OpenAiClientService openAiClientService;

	@Mock
	private AiSystemPromptService systemPromptService;

	@Mock
	private AiChatThreadRepository threadRepository;

	@Mock
	private AiChatMessageRepository messageRepository;

	@Mock
	private AiOpenAiToolCatalog toolCatalog;

	@Mock
	private AiOrchestrationToolDispatcher toolDispatcher;

	@Mock
	private TransactionTemplate transactionTemplate;

	@Mock
	private AiUserMessageGuard userMessageGuard;

	@Mock
	private AiChatPersistence chatPersistence;

	@Mock
	private AiOrchestrationTools orchestrationTools;

	@Mock
	private AiRequestScopePipeline requestScopePipeline;

	private AiRequestScopeGuard scopeGuard;

	private AiOrchestrationGuardrails realGuardrails;

	private AiUserMessageGuard realUserMessageGuard;

	private AiChatThread thread;

	@BeforeEach
	void setUp() {
		thread = new AiChatThread();
		thread.setId(THREAD_ID);
		thread.setNutritionistId(NUTRITIONIST_ID);
		final AiProperties guardProperties = new AiProperties();
		scopeGuard = new AiRequestScopeGuard(guardProperties);
		realGuardrails = new AiOrchestrationGuardrails(new AiToolAllowlist(new AiOpenAiToolCatalog()),
				new AiToolResultSanitizer(), new AiAssistantOutputValidator());
		realUserMessageGuard = new AiUserMessageGuard(guardProperties);
		when(userMessageGuard.validateAndSanitize(any(String.class)))
			.thenAnswer(invocation -> realUserMessageGuard.validateAndSanitize(invocation.getArgument(0)));
		when(userMessageGuard.wrapForModel(any(String.class)))
			.thenAnswer(invocation -> realUserMessageGuard.wrapForModel(invocation.getArgument(0)));
		when(chatPersistence.getThreadRepository()).thenReturn(threadRepository);
		when(chatPersistence.getMessageRepository()).thenReturn(messageRepository);
		when(chatPersistence.getTransactionTemplate()).thenReturn(transactionTemplate);
		when(orchestrationTools.getToolCatalog()).thenReturn(toolCatalog);
		when(orchestrationTools.getToolDispatcher()).thenReturn(toolDispatcher);
		when(orchestrationTools.getGuardrails()).thenReturn(realGuardrails);
		when(orchestrationTools.getAuditLogger()).thenReturn(AiMetricsTestSupport.auditLogger());
		when(requestScopePipeline.evaluate(any(String.class))).thenReturn(Optional.empty());
		when(transactionTemplate.execute(org.mockito.ArgumentMatchers.<TransactionCallback<Object>>any()))
			.thenAnswer(invocation -> {
				final TransactionCallback<?> callback = invocation.getArgument(0);
				return callback.doInTransaction(null);
			});
		when(messageRepository.save(any(AiChatMessage.class))).thenAnswer(invocation -> {
			final AiChatMessage saved = invocation.getArgument(0);
			if (saved.getId() == null) {
				saved.setId(100L);
			}
			return saved;
		});
	}

	static Stream<AiNutritionGoldenPrompt.Scenario> allScenarios() {
		return AiNutritionGoldenPrompt.scenarios();
	}

	static Stream<AiNutritionGoldenPrompt.Scenario> mustUseToolsScenarios() {
		return AiNutritionGoldenPrompt.mustUseToolsScenarios();
	}

	static Stream<AiNutritionGoldenPrompt.Scenario> mustClarifyScenarios() {
		return AiNutritionGoldenPrompt.mustClarifyScenarios();
	}

	static Stream<AiNutritionGoldenPrompt.Scenario> mustSearchCatalogScenarios() {
		return AiNutritionGoldenPrompt.mustSearchCatalogScenarios();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("allScenarios")
	void scenarioPassesScopeGuard(final AiNutritionGoldenPrompt.Scenario scenario) {
		assertThat(scopeGuard.evaluate(scenario.prompt())).as("scenario %s", scenario.id()).isEmpty();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("allScenarios")
	void expectedToolsAreRegistered(final AiNutritionGoldenPrompt.Scenario scenario) {
		final AiToolAllowlist allowlist = new AiToolAllowlist(new AiOpenAiToolCatalog());
		for (final String toolName : scenario.expectedTools()) {
			assertThat(allowlist.isAllowed(toolName)).as("scenario %s tool %s", scenario.id(), toolName).isTrue();
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("mustUseToolsScenarios")
	void simulatedToolLoopInvokesExpectedTools(final AiNutritionGoldenPrompt.Scenario scenario) {
		stubOperational();
		stubThreadAndHistory(scenario.prompt(), scenario.usesPatientContext());
		stubToolDispatchSuccess();
		final String assistantReply = buildAssistantReply(scenario.expectedWarningFragments());
		when(openAiClientService.chatCompletion(any())).thenReturn(toolCallsResponse(scenario.expectedTools()))
			.thenReturn(assistantOnlyResponse(assistantReply));

		final AiOrchestrationResult result = service.processUserMessage(orchestrationContext(scenario),
				scenario.prompt());

		for (final String toolName : scenario.expectedTools()) {
			verify(toolDispatcher).dispatch(any(), eq(toolName), any());
		}
		assertThat(result.toolCallsExecuted()).isEqualTo(scenario.expectedTools().size());
		assertWarningFragments(result.assistantMessage().getContent(), scenario);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("mustClarifyScenarios")
	void clarifyScenarioDoesNotInvokeTools(final AiNutritionGoldenPrompt.Scenario scenario) {
		stubOperational();
		stubThreadAndHistory(scenario.prompt(), false);
		final String clarifyReply = "¿Cuál es tu objetivo calórico (kcal) para este menú de un día?";
		when(openAiClientService.chatCompletion(any())).thenReturn(assistantOnlyResponse(clarifyReply));

		final AiOrchestrationResult result = service.processUserMessage(orchestrationContext(scenario),
				scenario.prompt());

		verify(toolDispatcher, never()).dispatch(any(), any(), any());
		assertThat(result.toolCallsExecuted()).isZero();
		assertWarningFragments(clarifyReply, scenario);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("mustSearchCatalogScenarios")
	void unsupportedIngredientUsesSearchWithoutInventing(final AiNutritionGoldenPrompt.Scenario scenario) {
		stubOperational();
		stubThreadAndHistory(scenario.prompt(), false);
		when(toolDispatcher.dispatch(any(), eq(SearchFoodCatalogToolService.TOOL_NAME), any()))
			.thenReturn("{\"success\":true,\"data\":{\"items\":[]}}");
		final String assistantReply = "No se encontró pitahaya en el catálogo. "
				+ "Puedes agregarla manualmente o elegir un sustituto del catálogo.";
		when(openAiClientService.chatCompletion(any()))
			.thenReturn(toolCallsResponse(List.of(SearchFoodCatalogToolService.TOOL_NAME)))
			.thenReturn(assistantOnlyResponse(assistantReply));

		final AiOrchestrationResult result = service.processUserMessage(orchestrationContext(scenario),
				scenario.prompt());

		verify(toolDispatcher).dispatch(any(), eq(SearchFoodCatalogToolService.TOOL_NAME), any());
		verify(toolDispatcher, never()).dispatch(any(), eq(CreateDishDraftToolService.TOOL_NAME), any());
		assertWarningFragments(result.assistantMessage().getContent(), scenario);
	}

	private void stubOperational() {
		when(properties.isOperational()).thenReturn(true);
		when(properties.getMaxToolCalls()).thenReturn(8);
		when(openAiClientService.isAvailable()).thenReturn(true);
		when(systemPromptService.buildSystemPrompt(any())).thenReturn("Eres un asistente nutricional.");
		when(toolCatalog.definitions()).thenReturn(List.of());
	}

	private void stubThreadAndHistory(final String userPrompt, final boolean withPatient) {
		when(threadRepository.findByIdAndNutritionistId(THREAD_ID, NUTRITIONIST_ID)).thenReturn(Optional.of(thread));
		when(messageRepository.findByThreadIdOrderByCreatedAtAscIdAsc(THREAD_ID))
			.thenReturn(List.of(userMessage(userPrompt)));
		if (withPatient) {
			when(systemPromptService.buildSystemPrompt(any())).thenReturn("Prompt con alergias: Huevo");
		}
	}

	private void stubToolDispatchSuccess() {
		when(toolDispatcher.dispatch(any(), any(), any())).thenReturn("{\"success\":true,\"data\":{}}");
	}

	private static OpenAiChatCompletionResponse toolCallsResponse(final List<String> toolNames) {
		final List<OpenAiToolCall> calls = IntStream.range(0, toolNames.size())
			.mapToObj(index -> new OpenAiToolCall("call_" + index, toolNames.get(index), "{}"))
			.toList();
		return new OpenAiChatCompletionResponse("id-tools", "assistant", null, calls, "tool_calls",
				new OpenAiTokenUsage(10, 5, 15));
	}

	private static OpenAiChatCompletionResponse assistantOnlyResponse(final String content) {
		return new OpenAiChatCompletionResponse("id-final", "assistant", content, List.of(), "stop",
				new OpenAiTokenUsage(20, 10, 30));
	}

	private static String buildAssistantReply(final List<String> warningFragments) {
		final StringBuilder reply = new StringBuilder("Borrador IA — revisión del nutriólogo requerida. ");
		for (final String fragment : warningFragments) {
			reply.append(fragment).append(". ");
		}
		return reply.toString().trim();
	}

	private static void assertWarningFragments(final String content, final AiNutritionGoldenPrompt.Scenario scenario) {
		final String normalized = content.toLowerCase(Locale.ROOT);
		for (final String fragment : scenario.expectedWarningFragments()) {
			assertThat(normalized).as("scenario %s", scenario.id()).contains(fragment.toLowerCase(Locale.ROOT));
		}
	}

	private static AiOrchestrationContext orchestrationContext(final AiNutritionGoldenPrompt.Scenario scenario) {
		if (!scenario.usesPatientContext()) {
			return new AiOrchestrationContext(NUTRITIONIST_ID, THREAD_ID, null, null, null);
		}
		final AiPatientPromptContext patient = new AiPatientPromptContext(42L, 1800.0, null, false, "F", false,
				"NORMAL", 23.5, Map.of(), "Huevo", "MODERATE", null, null, null);
		return new AiOrchestrationContext(NUTRITIONIST_ID, THREAD_ID, patient, null, null);
	}

	private static AiChatMessage userMessage(final String content) {
		final AiChatMessage message = new AiChatMessage();
		message.setRole(AiChatMessageRole.USER);
		message.setContent(content);
		return message;
	}

}
