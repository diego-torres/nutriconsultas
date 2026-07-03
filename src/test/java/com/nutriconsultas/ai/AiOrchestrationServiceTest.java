package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiOrchestrationServiceTest {

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

	private AiChatThread thread;

	@BeforeEach
	void setUp() {
		thread = new AiChatThread();
		thread.setId(THREAD_ID);
		thread.setNutritionistId(NUTRITIONIST_ID);
	}

	private void stubAiEnabled() {
		when(properties.isOperational()).thenReturn(true);
		when(openAiClientService.isAvailable()).thenReturn(true);
	}

	private void stubOperational() {
		stubAiEnabled();
		when(systemPromptService.buildSystemPrompt(any())).thenReturn("Eres un asistente nutricional.");
		when(toolCatalog.definitions()).thenReturn(List.of());
		when(messageRepository.save(any(AiChatMessage.class))).thenAnswer(invocation -> {
			final AiChatMessage message = invocation.getArgument(0);
			if (message.getId() == null) {
				message.setId(100L);
			}
			return message;
		});
	}

	@Test
	void processUserMessageReturnsAssistantReply() {
		stubOperational();
		when(threadRepository.findByIdAndNutritionistId(THREAD_ID, NUTRITIONIST_ID)).thenReturn(Optional.of(thread));
		when(messageRepository.findByThreadIdOrderByCreatedAtAscIdAsc(THREAD_ID))
			.thenReturn(List.of(message(AiChatMessageRole.USER, "Necesito un menú bajo en sodio")));
		when(openAiClientService.chatCompletion(any())).thenReturn(new OpenAiChatCompletionResponse("id-1", "assistant",
				"Aquí tienes una sugerencia.", List.of(), "stop", new OpenAiTokenUsage(20, 10, 30)));

		final AiOrchestrationResult result = service.processUserMessage(context(), "Necesito un menú bajo en sodio");

		assertThat(result.threadId()).isEqualTo(THREAD_ID);
		assertThat(result.toolCallsExecuted()).isZero();
		assertThat(result.assistantMessage().getRole()).isEqualTo(AiChatMessageRole.ASSISTANT);
		assertThat(result.assistantMessage().getContent()).isEqualTo("Aquí tienes una sugerencia.");
		assertThat(result.tokenUsage().totalTokens()).isEqualTo(30);
		verify(messageRepository, times(2)).save(any(AiChatMessage.class));
	}

	@Test
	void processUserMessageExecutesToolLoop() {
		stubOperational();
		when(properties.getMaxToolCalls()).thenReturn(8);
		when(threadRepository.findByIdAndNutritionistId(THREAD_ID, NUTRITIONIST_ID)).thenReturn(Optional.of(thread));
		when(messageRepository.findByThreadIdOrderByCreatedAtAscIdAsc(THREAD_ID))
			.thenReturn(List.of(message(AiChatMessageRole.USER, "Busca avena")));
		when(openAiClientService.chatCompletion(any()))
			.thenReturn(new OpenAiChatCompletionResponse("id-tools", "assistant", null,
					List.of(new OpenAiToolCall("call_1", SearchFoodCatalogToolService.TOOL_NAME,
							"{\"query\":\"avena\"}")),
					"tool_calls", new OpenAiTokenUsage(15, 5, 20)))
			.thenReturn(new OpenAiChatCompletionResponse("id-final", "assistant",
					"Encontré avena en el catálogo con esos datos.", List.of(), "stop",
					new OpenAiTokenUsage(40, 20, 60)));
		when(toolDispatcher.dispatch(any(), eq(SearchFoodCatalogToolService.TOOL_NAME), any()))
			.thenReturn("{\"success\":true,\"data\":{\"items\":[]}}");

		final AiOrchestrationResult result = service.processUserMessage(context(), "Busca avena");

		assertThat(result.toolCallsExecuted()).isEqualTo(1);
		assertThat(result.assistantMessage().getContent()).contains("Encontré avena");
		assertThat(result.tokenUsage().totalTokens()).isEqualTo(80);
		verify(toolDispatcher).dispatch(any(), eq(SearchFoodCatalogToolService.TOOL_NAME), any());
		verify(messageRepository, times(3)).save(any(AiChatMessage.class));
	}

	@Test
	void processUserMessageEnforcesMaxToolCalls() {
		stubOperational();
		when(properties.getMaxToolCalls()).thenReturn(1);
		when(threadRepository.findByIdAndNutritionistId(THREAD_ID, NUTRITIONIST_ID)).thenReturn(Optional.of(thread));
		when(messageRepository.findByThreadIdOrderByCreatedAtAscIdAsc(THREAD_ID))
			.thenReturn(List.of(message(AiChatMessageRole.USER, "Compara alimentos")));
		when(openAiClientService.chatCompletion(any())).thenReturn(new OpenAiChatCompletionResponse("id-tools",
				"assistant", null,
				List.of(new OpenAiToolCall("call_1", SearchFoodCatalogToolService.TOOL_NAME, "{\"query\":\"avena\"}"),
						new OpenAiToolCall("call_2", SearchFoodCatalogToolService.TOOL_NAME, "{\"query\":\"arroz\"}")),
				"tool_calls", null))
			.thenReturn(
					new OpenAiChatCompletionResponse(
							"id-more", "assistant", null, List.of(new OpenAiToolCall("call_3",
									SearchFoodCatalogToolService.TOOL_NAME, "{\"query\":\"frijol\"}")),
							"tool_calls", null));
		when(toolDispatcher.dispatch(any(), any(), any())).thenReturn("{\"success\":true}");

		final AiOrchestrationResult result = service.processUserMessage(context(), "Compara alimentos");

		assertThat(result.toolCallsExecuted()).isEqualTo(1);
		assertThat(result.assistantMessage().getContent()).contains("límite de consultas");
		verify(toolDispatcher, times(1)).dispatch(any(), any(), any());
	}

	@Test
	void processUserMessageWhenThreadNotFound() {
		stubAiEnabled();
		when(threadRepository.findByIdAndNutritionistId(THREAD_ID, NUTRITIONIST_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.processUserMessage(context(), "Hola"))
			.isInstanceOf(AiOrchestrationException.class)
			.hasMessageContaining("No se encontró la conversación");
	}

	@Test
	void processUserMessageWhenNotOperational() {
		when(properties.isOperational()).thenReturn(false);
		when(properties.isEnabledButMisconfigured()).thenReturn(true);
		when(properties.getMisconfigurationUserMessage()).thenReturn("El asistente de IA no está disponible.");

		assertThatThrownBy(() -> service.processUserMessage(context(), "Hola"))
			.isInstanceOf(AiOrchestrationException.class)
			.hasMessageContaining("no está disponible");
	}

	@Test
	void processUserMessageIncludesPersistedHistory() {
		stubOperational();
		final AiChatMessage priorUser = message(AiChatMessageRole.USER, "Mensaje anterior");
		final AiChatMessage priorAssistant = message(AiChatMessageRole.ASSISTANT, "Respuesta anterior");
		when(threadRepository.findByIdAndNutritionistId(THREAD_ID, NUTRITIONIST_ID)).thenReturn(Optional.of(thread));
		when(messageRepository.findByThreadIdOrderByCreatedAtAscIdAsc(THREAD_ID))
			.thenReturn(List.of(priorUser, priorAssistant, message(AiChatMessageRole.USER, "Siguiente pregunta")));
		when(openAiClientService.chatCompletion(any())).thenReturn(
				new OpenAiChatCompletionResponse("id-1", "assistant", "Continuación.", List.of(), "stop", null));

		service.processUserMessage(context(), "Siguiente pregunta");

		final ArgumentCaptor<OpenAiChatCompletionRequest> requestCaptor = ArgumentCaptor
			.forClass(OpenAiChatCompletionRequest.class);
		verify(openAiClientService).chatCompletion(requestCaptor.capture());
		final List<OpenAiChatMessage> messages = requestCaptor.getValue().messages();
		assertThat(messages).anyMatch(message -> "Mensaje anterior".equals(message.content()));
		assertThat(messages).anyMatch(message -> "Respuesta anterior".equals(message.content()));
		assertThat(messages).anyMatch(message -> "Siguiente pregunta".equals(message.content()));
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
