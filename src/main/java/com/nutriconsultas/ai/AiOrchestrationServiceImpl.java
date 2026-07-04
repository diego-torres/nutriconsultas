package com.nutriconsultas.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AiOrchestrationServiceImpl implements AiOrchestrationService {

	private static final int TOOL_AUDIT_MAX_CHARS = 2_000;

	private static final String TOOL_LIMIT_MESSAGE = "Alcancé el límite de consultas al catálogo en este turno. "
			+ "Responde con la información disponible hasta ahora.";

	private final AiProperties properties;

	private final OpenAiClientService openAiClientService;

	private final AiSystemPromptService systemPromptService;

	private final AiChatThreadRepository threadRepository;

	private final AiChatMessageRepository messageRepository;

	private final AiOpenAiToolCatalog toolCatalog;

	private final AiOrchestrationToolDispatcher toolDispatcher;

	private final TransactionTemplate transactionTemplate;

	private static final int STREAM_CHUNK_SIZE = 32;

	public AiOrchestrationServiceImpl(final AiProperties properties, final OpenAiClientService openAiClientService,
			final AiSystemPromptService systemPromptService, final AiChatThreadRepository threadRepository,
			final AiChatMessageRepository messageRepository, final AiOpenAiToolCatalog toolCatalog,
			final AiOrchestrationToolDispatcher toolDispatcher, final TransactionTemplate transactionTemplate) {
		this.properties = properties;
		this.openAiClientService = openAiClientService;
		this.systemPromptService = systemPromptService;
		this.threadRepository = threadRepository;
		this.messageRepository = messageRepository;
		this.toolCatalog = toolCatalog;
		this.toolDispatcher = toolDispatcher;
		this.transactionTemplate = transactionTemplate;
	}

	@Override
	@Transactional
	public AiOrchestrationResult processUserMessage(final AiOrchestrationContext context, final String userMessage) {
		assertOperational();
		validateUserMessage(userMessage);
		final AiChatThread thread = loadThread(context);
		persistUserMessage(thread, userMessage.trim());

		final List<OpenAiChatMessage> conversation = buildConversation(context, thread);
		final ToolLoopOutcome loopOutcome = runToolLoop(context, conversation, null);
		final AiChatMessage assistantMessage = persistAssistantMessage(thread, loopOutcome.assistantContent());
		persistToolAuditMessages(thread, loopOutcome.toolAuditEntries());
		touchThread(thread);

		if (log.isInfoEnabled()) {
			log.info("AI orchestration threadId={} toolCalls={}", thread.getId(), loopOutcome.toolCallsExecuted());
		}
		return new AiOrchestrationResult(thread.getId(), assistantMessage, loopOutcome.toolCallsExecuted(),
				loopOutcome.tokenUsage());
	}

	@Override
	public void processUserMessageStreaming(final AiOrchestrationContext context, final String userMessage,
			final AiStreamEventConsumer streamConsumer) {
		assertOperational();
		validateUserMessage(userMessage);
		final AiChatThread thread = transactionTemplate.execute(status -> {
			final AiChatThread loaded = loadThread(context);
			persistUserMessage(loaded, userMessage.trim());
			return loaded;
		});
		if (thread == null) {
			throw new AiOrchestrationException("No se pudo guardar el mensaje.");
		}

		final List<OpenAiChatMessage> conversation = transactionTemplate.execute(status -> {
			final AiChatThread loadedThread = loadThread(context);
			return buildConversation(context, loadedThread);
		});
		if (conversation == null) {
			throw new AiOrchestrationException("No se pudo cargar el historial de la conversación.");
		}
		streamConsumer.onStatus("thinking", "El asistente está pensando…");
		streamConsumer.throwIfCancelled();
		final ToolLoopOutcome loopOutcome = runToolLoop(context, conversation, streamConsumer);
		streamConsumer.throwIfCancelled();
		emitContentDeltas(loopOutcome.assistantContent(), streamConsumer);
		streamConsumer.throwIfCancelled();

		final AiOrchestrationResult result = transactionTemplate.execute(status -> {
			final AiChatMessage assistantMessage = persistAssistantMessage(thread, loopOutcome.assistantContent());
			persistToolAuditMessages(thread, loopOutcome.toolAuditEntries());
			touchThread(thread);
			if (log.isInfoEnabled()) {
				log.info("AI streaming orchestration threadId={} toolCalls={}", thread.getId(),
						loopOutcome.toolCallsExecuted());
			}
			return new AiOrchestrationResult(thread.getId(), assistantMessage, loopOutcome.toolCallsExecuted(),
					loopOutcome.tokenUsage());
		});
		if (result == null) {
			throw new AiOrchestrationException("No se pudo guardar la respuesta del asistente.");
		}
		streamConsumer.onComplete(result);
	}

	private void emitContentDeltas(final String content, final AiStreamEventConsumer streamConsumer) {
		if (!StringUtils.hasText(content)) {
			return;
		}
		for (int index = 0; index < content.length(); index += STREAM_CHUNK_SIZE) {
			streamConsumer.throwIfCancelled();
			final int end = Math.min(index + STREAM_CHUNK_SIZE, content.length());
			streamConsumer.onDelta(content.substring(index, end));
		}
	}

	private void assertOperational() {
		if (!properties.isOperational()) {
			if (properties.isEnabledButMisconfigured()) {
				throw new AiOrchestrationException(properties.getMisconfigurationUserMessage());
			}
			throw new AiOrchestrationException("El asistente de IA no está habilitado.");
		}
		if (!openAiClientService.isAvailable()) {
			throw new AiOrchestrationException(properties.getMisconfigurationUserMessage());
		}
	}

	private void validateUserMessage(final String userMessage) {
		if (!StringUtils.hasText(userMessage)) {
			throw new AiOrchestrationException("El mensaje no puede estar vacío.");
		}
	}

	private AiChatThread loadThread(final AiOrchestrationContext context) {
		return threadRepository.findByIdAndNutritionistId(context.threadId(), context.nutritionistId())
			.orElseThrow(() -> new AiOrchestrationException("No se encontró la conversación."));
	}

	private void persistUserMessage(final AiChatThread thread, final String content) {
		final AiChatMessage message = new AiChatMessage();
		message.setThread(thread);
		message.setRole(AiChatMessageRole.USER);
		message.setContent(content);
		messageRepository.save(message);
	}

	private List<OpenAiChatMessage> buildConversation(final AiOrchestrationContext context, final AiChatThread thread) {
		final AiSystemPromptContext promptContext = new AiSystemPromptContext(
				Locale.forLanguageTag(AiSystemPromptContext.DEFAULT_LOCALE_TAG),
				"El nutriólogo autenticado es el único dueño de los datos y borradores de esta sesión.",
				context.patientContext(), context.dietaContext(), context.platilloContext());
		final List<OpenAiChatMessage> messages = new ArrayList<>();
		messages.add(OpenAiChatMessage.system(systemPromptService.buildSystemPrompt(promptContext)));
		appendPersistedHistory(messages, thread.getId());
		return messages;
	}

	private void appendPersistedHistory(final List<OpenAiChatMessage> messages, final long threadId) {
		final List<AiChatMessage> persisted = messageRepository.findByThreadIdOrderByCreatedAtAscIdAsc(threadId);
		for (final AiChatMessage message : persisted) {
			if (message.getRole() == AiChatMessageRole.USER) {
				messages.add(OpenAiChatMessage.user(message.getContent()));
			}
			else if (message.getRole() == AiChatMessageRole.ASSISTANT) {
				messages.add(OpenAiChatMessage.assistant(message.getContent()));
			}
		}
	}

	private ToolLoopOutcome runToolLoop(final AiOrchestrationContext context,
			final List<OpenAiChatMessage> conversation, final AiStreamEventConsumer streamConsumer) {
		int toolCallsExecuted = 0;
		OpenAiTokenUsage accumulatedUsage = null;
		final List<ToolAuditEntry> toolAuditEntries = new ArrayList<>();
		String assistantContent = null;

		while (true) {
			if (streamConsumer != null) {
				streamConsumer.throwIfCancelled();
			}
			final OpenAiChatCompletionResponse response = openAiClientService
				.chatCompletion(new OpenAiChatCompletionRequest(List.copyOf(conversation), toolCatalog.definitions()));
			accumulatedUsage = mergeUsage(accumulatedUsage, response.usage());
			assistantContent = response.content();

			if (!response.hasToolCalls()) {
				break;
			}
			if (streamConsumer != null) {
				streamConsumer.onStatus("tools", "Consultando catálogo nutricional…");
			}
			if (toolCallsExecuted >= properties.getMaxToolCalls()) {
				assistantContent = TOOL_LIMIT_MESSAGE;
				if (log.isWarnEnabled()) {
					log.warn("AI orchestration threadId={} reached maxToolCalls={}", context.threadId(),
							properties.getMaxToolCalls());
				}
				break;
			}

			conversation.add(OpenAiChatMessage.assistantWithToolCalls(response.content(), response.toolCalls()));
			for (final OpenAiToolCall toolCall : response.toolCalls()) {
				if (toolCallsExecuted >= properties.getMaxToolCalls()) {
					break;
				}
				final String toolResultJson = executeToolCall(context, toolCall);
				conversation.add(OpenAiChatMessage.tool(toolCall.id(), toolCall.name(), toolResultJson));
				toolAuditEntries.add(new ToolAuditEntry(toolCall.name(), toolResultJson));
				toolCallsExecuted++;
			}
		}

		if (!StringUtils.hasText(assistantContent)) {
			assistantContent = "No pude generar una respuesta en este momento. Intenta reformular tu solicitud.";
		}
		return new ToolLoopOutcome(assistantContent, toolCallsExecuted, accumulatedUsage, toolAuditEntries);
	}

	private String executeToolCall(final AiOrchestrationContext context, final OpenAiToolCall toolCall) {
		try {
			return toolDispatcher.dispatch(context, toolCall.name(), toolCall.argumentsJson());
		}
		catch (final AiOrchestrationException ex) {
			if (log.isWarnEnabled()) {
				log.warn("AI tool dispatch failed tool={} threadId={}", toolCall.name(), context.threadId());
			}
			return AiToolJsonSerializer.toJson(AiToolResult.error(AiToolErrorCode.VALIDATION, ex.getMessage()));
		}
	}

	private AiChatMessage persistAssistantMessage(final AiChatThread thread, final String content) {
		final AiChatMessage message = new AiChatMessage();
		message.setThread(thread);
		message.setRole(AiChatMessageRole.ASSISTANT);
		message.setContent(content);
		return messageRepository.save(message);
	}

	private void persistToolAuditMessages(final AiChatThread thread, final List<ToolAuditEntry> entries) {
		for (final ToolAuditEntry entry : entries) {
			final AiChatMessage message = new AiChatMessage();
			message.setThread(thread);
			message.setRole(AiChatMessageRole.TOOL);
			message.setToolName(entry.toolName());
			message.setContent(truncateForAudit(entry.resultJson()));
			messageRepository.save(message);
		}
	}

	private void touchThread(final AiChatThread thread) {
		threadRepository.save(thread);
	}

	private static String truncateForAudit(final String json) {
		if (json.length() <= TOOL_AUDIT_MAX_CHARS) {
			return json;
		}
		return json.substring(0, TOOL_AUDIT_MAX_CHARS) + "...";
	}

	private static OpenAiTokenUsage mergeUsage(final OpenAiTokenUsage accumulated, final OpenAiTokenUsage latest) {
		if (latest == null) {
			return accumulated;
		}
		if (accumulated == null) {
			return latest;
		}
		return new OpenAiTokenUsage(accumulated.promptTokens() + latest.promptTokens(),
				accumulated.completionTokens() + latest.completionTokens(),
				accumulated.totalTokens() + latest.totalTokens());
	}

	private record ToolAuditEntry(String toolName, String resultJson) {
	}

	private record ToolLoopOutcome(String assistantContent, int toolCallsExecuted, OpenAiTokenUsage tokenUsage,
			List<ToolAuditEntry> toolAuditEntries) {
	}

}
