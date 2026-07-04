package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiSecurityGoldenPromptTest {

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

	private AiOrchestrationGuardrails realGuardrails;

	private AiUserMessageGuard realUserMessageGuard;

	private AiChatThread thread;

	@BeforeEach
	void setUp() {
		thread = new AiChatThread();
		thread.setId(THREAD_ID);
		thread.setNutritionistId(NUTRITIONIST_ID);
		realGuardrails = new AiOrchestrationGuardrails(new AiToolAllowlist(new AiOpenAiToolCatalog()),
				new AiToolResultSanitizer(), new AiAssistantOutputValidator());
		final AiProperties guardProperties = new AiProperties();
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
		when(requestScopePipeline.evaluate(any(String.class))).thenReturn(Optional.empty());
		when(transactionTemplate.execute(org.mockito.ArgumentMatchers.<TransactionCallback<Object>>any()))
			.thenAnswer(invocation -> {
				final TransactionCallback<?> callback = invocation.getArgument(0);
				return callback.doInTransaction(null);
			});
	}

	static Stream<AiSecurityGoldenPrompt.Scenario> toolAllowlistScenarios() {
		return AiSecurityGoldenPrompt.toolAllowlistScenarios();
	}

	static Stream<AiSecurityGoldenPrompt.Scenario> outputRedactionScenarios() {
		return AiSecurityGoldenPrompt.outputRedactionScenarios();
	}

	static Stream<AiSecurityGoldenPrompt.Scenario> toolResultScenarios() {
		return AiSecurityGoldenPrompt.toolResultScenarios();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("toolAllowlistScenarios")
	void unknownToolIsRejectedBeforeDispatch(final AiSecurityGoldenPrompt.Scenario scenario) {
		stubOperational();
		when(threadRepository.findByIdAndNutritionistId(THREAD_ID, NUTRITIONIST_ID)).thenReturn(Optional.of(thread));
		when(messageRepository.findByThreadIdOrderByCreatedAtAscIdAsc(THREAD_ID))
			.thenReturn(List.of(message(AiChatMessageRole.USER, "Consulta")));
		when(openAiClientService.chatCompletion(any()))
			.thenReturn(new OpenAiChatCompletionResponse("id-tools", "assistant", null,
					List.of(new OpenAiToolCall("call_1", scenario.input(), "{}")), "tool_calls",
					new OpenAiTokenUsage(10, 5, 15)))
			.thenReturn(new OpenAiChatCompletionResponse("id-final", "assistant", "Listo.", List.of(), "stop", null));
		when(messageRepository.save(any(AiChatMessage.class))).thenAnswer(invocation -> {
			final AiChatMessage saved = invocation.getArgument(0);
			if (saved.getId() == null) {
				saved.setId(100L);
			}
			return saved;
		});

		service.processUserMessage(context(), "Consulta");

		verify(toolDispatcher, never()).dispatch(any(), eq(scenario.input()), any());
		final ArgumentCaptor<OpenAiChatCompletionRequest> requestCaptor = ArgumentCaptor
			.forClass(OpenAiChatCompletionRequest.class);
		verify(openAiClientService, org.mockito.Mockito.times(2)).chatCompletion(requestCaptor.capture());
		final List<OpenAiChatMessage> secondRequestMessages = requestCaptor.getAllValues().get(1).messages();
		assertThat(secondRequestMessages).anyMatch(message -> message.role().equals("tool") && message.content() != null
				&& message.content().contains(AiToolAllowlist.REJECTION_MESSAGE));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("outputRedactionScenarios")
	void assistantOutputIsRedactedBeforePersist(final AiSecurityGoldenPrompt.Scenario scenario) {
		stubOperational();
		when(threadRepository.findByIdAndNutritionistId(THREAD_ID, NUTRITIONIST_ID)).thenReturn(Optional.of(thread));
		when(messageRepository.findByThreadIdOrderByCreatedAtAscIdAsc(THREAD_ID))
			.thenReturn(List.of(message(AiChatMessageRole.USER, "Consulta")));
		when(openAiClientService.chatCompletion(any())).thenReturn(new OpenAiChatCompletionResponse("id-1", "assistant",
				"Dato filtrado: " + scenario.input(), List.of(), "stop", null));
		when(messageRepository.save(any(AiChatMessage.class))).thenAnswer(invocation -> {
			final AiChatMessage saved = invocation.getArgument(0);
			if (saved.getId() == null) {
				saved.setId(100L);
			}
			return saved;
		});

		final AiOrchestrationResult result = service.processUserMessage(context(), "Consulta");

		assertThat(result.assistantMessage().getContent()).doesNotContain(scenario.input());
		assertThat(result.assistantMessage().getContent()).containsAnyOf(AiAssistantOutputValidator.REDACTED_SECRET,
				AiAssistantOutputValidator.REDACTED_PII);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("toolResultScenarios")
	void toolResultsAreSanitizedBeforeSecondCompletion(final AiSecurityGoldenPrompt.Scenario scenario) {
		stubOperational();
		when(threadRepository.findByIdAndNutritionistId(THREAD_ID, NUTRITIONIST_ID)).thenReturn(Optional.of(thread));
		when(messageRepository.findByThreadIdOrderByCreatedAtAscIdAsc(THREAD_ID))
			.thenReturn(List.of(message(AiChatMessageRole.USER, "Busca alimento")));
		when(openAiClientService.chatCompletion(any()))
			.thenReturn(new OpenAiChatCompletionResponse("id-tools", "assistant", null,
					List.of(new OpenAiToolCall("call_1", SearchFoodCatalogToolService.TOOL_NAME,
							"{\"query\":\"avena\"}")),
					"tool_calls", null))
			.thenReturn(new OpenAiChatCompletionResponse("id-final", "assistant", "Resultado seguro.", List.of(),
					"stop", null));
		when(toolDispatcher.dispatch(any(), eq(SearchFoodCatalogToolService.TOOL_NAME), any()))
			.thenReturn(scenario.input());
		when(messageRepository.save(any(AiChatMessage.class))).thenAnswer(invocation -> {
			final AiChatMessage saved = invocation.getArgument(0);
			if (saved.getId() == null) {
				saved.setId(100L);
			}
			return saved;
		});

		service.processUserMessage(context(), "Busca alimento");

		final ArgumentCaptor<OpenAiChatCompletionRequest> requestCaptor = ArgumentCaptor
			.forClass(OpenAiChatCompletionRequest.class);
		verify(openAiClientService, org.mockito.Mockito.times(2)).chatCompletion(requestCaptor.capture());
		final List<OpenAiChatMessage> toolMessages = requestCaptor.getAllValues()
			.get(1)
			.messages()
			.stream()
			.filter(message -> "tool".equals(message.role()))
			.toList();
		assertThat(toolMessages).hasSize(1);
		assertThat(toolMessages.get(0).content()).contains(AiPromptDelimiters.TOOL_RESULT_OPEN);
		assertThat(toolMessages.get(0).content()).contains("[contenido filtrado por seguridad]");
	}

	private void stubOperational() {
		when(properties.isOperational()).thenReturn(true);
		when(properties.getMaxToolCalls()).thenReturn(8);
		when(openAiClientService.isAvailable()).thenReturn(true);
		when(systemPromptService.buildSystemPrompt(any())).thenReturn("Eres un asistente nutricional.");
		when(toolCatalog.definitions()).thenReturn(List.of());
	}

	private static AiOrchestrationContext context() {
		return new AiOrchestrationContext(NUTRITIONIST_ID, THREAD_ID, null, null, null);
	}

	private static AiChatMessage message(final AiChatMessageRole role, final String content) {
		final AiChatMessage message = new AiChatMessage();
		message.setRole(role);
		message.setContent(content);
		return message;
	}

}
