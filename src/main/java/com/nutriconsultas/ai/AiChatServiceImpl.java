package com.nutriconsultas.ai;

import java.util.ArrayList;
import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AiChatServiceImpl implements AiChatService {

	private static final int MAX_TITLE_LENGTH = 200;

	private static final String DEFAULT_TITLE = "Nueva conversación";

	private final AiChatPersistence chatPersistence;

	private final AiGeneratedDraftRepository draftRepository;

	private final AiOrchestrationService orchestrationService;

	private final AiChatPromptContextResolvers promptContextResolvers;

	private final PacienteRepository pacienteRepository;

	private final AiChatRequestGuards chatRequestGuards;

	private final AiAuditLogger auditLogger;

	public AiChatServiceImpl(final AiChatPersistence chatPersistence, final AiGeneratedDraftRepository draftRepository,
			final AiOrchestrationService orchestrationService,
			final AiChatPromptContextResolvers promptContextResolvers, final PacienteRepository pacienteRepository,
			final AiChatRequestGuards chatRequestGuards, final AiAuditLogger auditLogger) {
		this.chatPersistence = chatPersistence;
		this.draftRepository = draftRepository;
		this.orchestrationService = orchestrationService;
		this.promptContextResolvers = promptContextResolvers;
		this.pacienteRepository = pacienteRepository;
		this.chatRequestGuards = chatRequestGuards;
		this.auditLogger = auditLogger;
	}

	@Override
	@Transactional
	public AiChatThread startThread(@NonNull final String nutritionistId, @Nullable final String title,
			@Nullable final Long patientId, @Nullable final Long clinicId, final AiChatPromptContext promptContext) {
		assertNutritionistAccess(nutritionistId);
		final AiChatPromptContext resolvedPromptContext = mergePromptContext(promptContext, null);
		final Long linkedPatientId = patientId != null ? patientId : resolvedPromptContext.patientId();
		final Paciente patient = resolveOwnedPatient(linkedPatientId, nutritionistId);
		final AiChatThread thread = new AiChatThread();
		thread.setNutritionistId(nutritionistId);
		thread.setTitle(resolveTitle(title));
		thread.setClinicId(clinicId);
		thread.setPatient(patient);
		final AiChatThread saved = chatPersistence.getThreadRepository().save(thread);
		auditLogger.logThreadCreated(saved.getId(), nutritionistId, patient != null);
		return saved;
	}

	@Override
	@Transactional(readOnly = true)
	public AiChatThreadDetail getThread(@NonNull final String nutritionistId, final long threadId) {
		assertNutritionistAccess(nutritionistId);
		final AiChatThread thread = loadOwnedThread(threadId, nutritionistId);
		final List<AiChatMessageView> messages = toVisibleMessages(
				chatPersistence.getMessageRepository().findByThreadIdOrderByCreatedAtAscIdAsc(threadId));
		return new AiChatThreadDetail(thread.getId(), thread.getTitle(), patientId(thread), thread.getClinicId(),
				thread.getCreatedAt(), thread.getUpdatedAt(), messages);
	}

	@Override
	@Transactional(readOnly = true)
	public AiChatDraftList listDrafts(@NonNull final String nutritionistId, final long threadId) {
		assertNutritionistAccess(nutritionistId);
		loadOwnedThread(threadId, nutritionistId);
		final List<AiChatDraftSummary> drafts = new ArrayList<>();
		for (final AiGeneratedDraft draft : draftRepository.findByThreadIdOrderByCreatedAtDescIdDesc(threadId)) {
			drafts.add(new AiChatDraftSummary(draft.getId(), draft.getDraftType(), draft.getStatus(),
					AiDraftSummaryExtractor.summarize(draft), draft.getCreatedAt()));
		}
		return new AiChatDraftList(threadId, List.copyOf(drafts));
	}

	@Override
	@Transactional
	public AiOrchestrationResult sendMessage(@NonNull final String nutritionistId, final long threadId,
			final String message, final AiChatPromptContext promptContext) {
		assertNutritionistAccess(nutritionistId);
		if (!StringUtils.hasText(message)) {
			throw new AiChatException(org.springframework.http.HttpStatus.BAD_REQUEST, AiToolErrorCode.VALIDATION,
					"El mensaje no puede estar vacío.");
		}
		final String sanitizedMessage = validateUserMessageContent(message);
		final AiChatThread thread = loadOwnedThread(threadId, nutritionistId);
		final AiChatPromptContext mergedContext = mergePromptContext(promptContext, patientId(thread));
		auditChatRequest(nutritionistId, threadId, AiChatRequestMode.SEND, sanitizedMessage, mergedContext);
		final AiOrchestrationContext context = buildOrchestrationContext(nutritionistId, threadId, mergedContext);
		return orchestrationService.processUserMessage(context, sanitizedMessage);
	}

	@Override
	public void streamMessage(@NonNull final String nutritionistId, final AiSendMessageRequest request,
			final SseEmitter emitter) {
		assertNutritionistAccess(nutritionistId);
		if (request == null || !StringUtils.hasText(request.message())) {
			completeStreamWithError(emitter, AiToolErrorCode.VALIDATION, AiErrorMessages.EMPTY_MESSAGE);
			return;
		}
		try {
			final String sanitizedMessage = validateUserMessageContent(request.message());
			final AiStreamCancellation cancellation = new AiStreamCancellation();
			emitter.onCompletion(cancellation::cancel);
			emitter.onTimeout(cancellation::cancel);
			emitter.onError(ex -> cancellation.cancel());
			final AiChatPromptContext promptContext = new AiChatPromptContext(request.patientId(), request.dietaId(),
					request.platilloId());
			final Long threadPatientId = chatPersistence.getTransactionTemplate().execute(status -> {
				final AiChatThread ownedThread = loadOwnedThread(request.threadId(), nutritionistId);
				return patientId(ownedThread);
			});
			final AiChatPromptContext mergedContext = mergePromptContext(promptContext, threadPatientId);
			auditChatRequest(nutritionistId, request.threadId(), AiChatRequestMode.STREAM, sanitizedMessage,
					mergedContext);
			final AiOrchestrationContext context = buildOrchestrationContext(nutritionistId, request.threadId(),
					mergedContext);
			orchestrationService.processUserMessageStreaming(context, sanitizedMessage,
					streamConsumerFor(emitter, cancellation));
			AiChatSseSupport.completeQuietly(emitter);
		}
		catch (final AiChatException ex) {
			completeStreamWithError(emitter, ex.getErrorCode(), ex.getMessage());
		}
		catch (final AiOrchestrationException ex) {
			completeStreamWithError(emitter, AiToolErrorCode.VALIDATION, ex.getMessage());
		}
		catch (final AiStreamCancelledException ex) {
			if (log.isDebugEnabled()) {
				log.debug("AI chat stream cancelled threadId={}", request.threadId());
			}
			AiChatSseSupport.completeQuietly(emitter);
		}
		catch (final OpenAiClientException ex) {
			completeStreamWithError(emitter, AiErrorMessages.errorCodeForOpenAi(ex.getKind()), ex.getUserMessage());
		}
		catch (final AiStreamDeliveryException ex) {
			if (log.isWarnEnabled()) {
				log.warn("AI chat stream delivery failed threadId={}", request.threadId());
			}
			AiChatSseSupport.completeQuietly(emitter);
		}
	}

	@Override
	@Transactional
	public AiEditResubmitResult editAndResubmitMessage(@NonNull final String nutritionistId,
			final AiEditMessageRequest request) {
		assertNutritionistAccess(nutritionistId);
		assertEditRequest(request);
		final String sanitizedMessage = validateUserMessageContent(request.message());
		final AiChatPromptContext promptContext = new AiChatPromptContext(request.patientId(), request.dietaId(),
				request.platilloId());
		final TruncateOutcome truncateOutcome = truncateThreadFromMessage(nutritionistId, request.threadId(),
				request.messageId());
		final AiChatThread thread = loadOwnedThread(request.threadId(), nutritionistId);
		final AiChatPromptContext mergedContext = mergePromptContext(promptContext, patientId(thread));
		auditChatRequest(nutritionistId, request.threadId(), AiChatRequestMode.EDIT, sanitizedMessage, mergedContext);
		final AiOrchestrationContext context = buildOrchestrationContext(nutritionistId, request.threadId(),
				mergedContext);
		final AiOrchestrationResult orchestration = orchestrationService.processUserMessage(context, sanitizedMessage);
		return new AiEditResubmitResult(orchestration, truncateOutcome.truncatedMessageCount(),
				truncateOutcome.discardedDraftIds());
	}

	@Override
	public void streamEditMessage(@NonNull final String nutritionistId, final AiEditMessageRequest request,
			final SseEmitter emitter) {
		assertNutritionistAccess(nutritionistId);
		if (request == null) {
			completeStreamWithError(emitter, AiToolErrorCode.VALIDATION, AiErrorMessages.INVALID_REQUEST);
			return;
		}
		try {
			assertEditRequest(request);
			final String sanitizedMessage = validateUserMessageContent(request.message());
			final AiStreamCancellation cancellation = new AiStreamCancellation();
			emitter.onCompletion(cancellation::cancel);
			emitter.onTimeout(cancellation::cancel);
			emitter.onError(ex -> cancellation.cancel());
			final AiChatPromptContext promptContext = new AiChatPromptContext(request.patientId(), request.dietaId(),
					request.platilloId());
			final TruncateOutcome truncateOutcome = chatPersistence.getTransactionTemplate()
				.execute(status -> truncateThreadFromMessage(nutritionistId, request.threadId(), request.messageId()));
			if (truncateOutcome == null) {
				completeStreamWithError(emitter, AiToolErrorCode.INTERNAL, AiErrorMessages.THREAD_UPDATE_FAILED);
				return;
			}
			final Long threadPatientId = chatPersistence.getTransactionTemplate().execute(status -> {
				final AiChatThread ownedThread = loadOwnedThread(request.threadId(), nutritionistId);
				return patientId(ownedThread);
			});
			final AiChatPromptContext mergedContext = mergePromptContext(promptContext, threadPatientId);
			auditChatRequest(nutritionistId, request.threadId(), AiChatRequestMode.EDIT_STREAM, sanitizedMessage,
					mergedContext);
			final AiOrchestrationContext context = buildOrchestrationContext(nutritionistId, request.threadId(),
					mergedContext);
			orchestrationService.processUserMessageStreaming(context, sanitizedMessage,
					streamConsumerFor(emitter, cancellation));
			AiChatSseSupport.completeQuietly(emitter);
		}
		catch (final AiStreamCancelledException ex) {
			if (log.isDebugEnabled()) {
				log.debug("AI chat edit-resubmit stream cancelled threadId={}", request.threadId());
			}
			AiChatSseSupport.completeQuietly(emitter);
		}
		catch (final AiChatException ex) {
			completeStreamWithError(emitter, ex.getErrorCode(), ex.getMessage());
		}
		catch (final AiOrchestrationException ex) {
			completeStreamWithError(emitter, AiToolErrorCode.VALIDATION, ex.getMessage());
		}
		catch (final OpenAiClientException ex) {
			completeStreamWithError(emitter, AiErrorMessages.errorCodeForOpenAi(ex.getKind()), ex.getUserMessage());
		}
		catch (final AiStreamDeliveryException ex) {
			if (log.isWarnEnabled()) {
				log.warn("AI chat edit-resubmit stream delivery failed threadId={}", request.threadId());
			}
			AiChatSseSupport.completeQuietly(emitter);
		}
	}

	private AiStreamEventConsumer streamConsumerFor(final SseEmitter emitter, final AiStreamCancellation cancellation) {
		return new AiStreamEventConsumer() {
			@Override
			public boolean isCancelled() {
				return cancellation.isCancelled();
			}

			@Override
			public void onStatus(final String phase, final String message) {
				try {
					AiChatSseSupport.sendStatus(emitter, phase, message);
				}
				catch (Exception ex) {
					throw new AiStreamDeliveryException("Stream status failed", ex);
				}
			}

			@Override
			public void onDelta(final String contentDelta) {
				try {
					AiChatSseSupport.sendDelta(emitter, contentDelta);
				}
				catch (Exception ex) {
					throw new AiStreamDeliveryException("Stream delta failed", ex);
				}
			}

			@Override
			public void onComplete(final AiOrchestrationResult result) {
				try {
					AiChatSseSupport.sendDone(emitter, result);
				}
				catch (Exception ex) {
					throw new AiStreamDeliveryException("Stream done failed", ex);
				}
			}
		};
	}

	private static void completeStreamWithError(final SseEmitter emitter, final AiToolErrorCode errorCode,
			final String message) {
		try {
			AiChatSseSupport.sendError(emitter, errorCode, message);
		}
		catch (Exception ex) {
			emitter.completeWithError(ex);
			return;
		}
		AiChatSseSupport.completeQuietly(emitter);
	}

	private void auditChatRequest(final String nutritionistId, final long threadId, final AiChatRequestMode mode,
			final String sanitizedMessage, final AiChatPromptContext context) {
		auditLogger.logChatRequest(threadId, nutritionistId, mode, AiAuditRedaction.safeMessageLength(sanitizedMessage),
				context.patientId() != null, context.dietaId() != null, context.platilloId() != null);
	}

	private AiOrchestrationContext buildOrchestrationContext(final String nutritionistId, final long threadId,
			final AiChatPromptContext promptContext) {
		return promptContextResolvers.buildOrchestrationContext(nutritionistId, threadId, promptContext);
	}

	private static AiChatPromptContext mergePromptContext(final AiChatPromptContext requestContext,
			@Nullable final Long threadPatientId) {
		final AiChatPromptContext safeRequest = requestContext != null ? requestContext : AiChatPromptContext.empty();
		final Long patientId = safeRequest.patientId() != null ? safeRequest.patientId() : threadPatientId;
		return new AiChatPromptContext(patientId, safeRequest.dietaId(), safeRequest.platilloId());
	}

	private AiChatThread loadOwnedThread(final long threadId, final String nutritionistId) {
		return chatPersistence.getThreadRepository()
			.findByIdAndNutritionistId(threadId, nutritionistId)
			.orElseThrow(() -> new AiChatException(org.springframework.http.HttpStatus.NOT_FOUND,
					AiToolErrorCode.NOT_FOUND, "No se encontró la conversación."));
	}

	private Paciente resolveOwnedPatient(@Nullable final Long patientId, final String nutritionistId) {
		if (patientId == null) {
			return null;
		}
		return pacienteRepository.findByIdAndUserId(patientId, nutritionistId)
			.orElseThrow(() -> new AiChatException(org.springframework.http.HttpStatus.NOT_FOUND,
					AiToolErrorCode.NOT_FOUND, "No se encontró el paciente."));
	}

	private static Long patientId(final AiChatThread thread) {
		return thread.getPatient() != null ? thread.getPatient().getId() : null;
	}

	private static List<AiChatMessageView> toVisibleMessages(final List<AiChatMessage> persisted) {
		final List<AiChatMessageView> messages = new ArrayList<>();
		for (final AiChatMessage message : persisted) {
			if (message.getRole() == AiChatMessageRole.USER || message.getRole() == AiChatMessageRole.ASSISTANT) {
				messages.add(new AiChatMessageView(message.getId(), message.getRole(), message.getContent(),
						message.getCreatedAt()));
			}
		}
		return List.copyOf(messages);
	}

	private static String resolveTitle(@Nullable final String title) {
		if (!StringUtils.hasText(title)) {
			return DEFAULT_TITLE;
		}
		final String trimmed = title.trim();
		if (trimmed.length() <= MAX_TITLE_LENGTH) {
			return trimmed;
		}
		return trimmed.substring(0, MAX_TITLE_LENGTH);
	}

	private void assertNutritionistAccess(final String nutritionistId) {
		chatRequestGuards.assertNutritionistAccess(nutritionistId);
	}

	private static void assertEditRequest(final AiEditMessageRequest request) {
		if (request == null) {
			throw new AiChatException(org.springframework.http.HttpStatus.BAD_REQUEST, AiToolErrorCode.VALIDATION,
					"Solicitud no válida.");
		}
		if (!StringUtils.hasText(request.message())) {
			throw new AiChatException(org.springframework.http.HttpStatus.BAD_REQUEST, AiToolErrorCode.VALIDATION,
					"El mensaje no puede estar vacío.");
		}
	}

	private String validateUserMessageContent(final String message) {
		try {
			return chatRequestGuards.validateUserMessage(message);
		}
		catch (final AiOrchestrationException ex) {
			final AiChatException chatException = new AiChatException(org.springframework.http.HttpStatus.BAD_REQUEST,
					AiToolErrorCode.VALIDATION, ex.getMessage());
			chatException.initCause(ex);
			throw chatException;
		}
	}

	private TruncateOutcome truncateThreadFromMessage(final String nutritionistId, final long threadId,
			final long messageId) {
		final AiChatMessage anchor = chatPersistence.getMessageRepository()
			.findByIdAndThreadNutritionistId(messageId, nutritionistId)
			.orElseThrow(() -> new AiChatException(org.springframework.http.HttpStatus.NOT_FOUND,
					AiToolErrorCode.NOT_FOUND, "No se encontró el mensaje."));
		if (anchor.getThread() == null || anchor.getThread().getId() == null
				|| anchor.getThread().getId() != threadId) {
			throw new AiChatException(org.springframework.http.HttpStatus.BAD_REQUEST, AiToolErrorCode.VALIDATION,
					"El mensaje no pertenece a esta conversación.");
		}
		if (anchor.getRole() != AiChatMessageRole.USER) {
			throw new AiChatException(org.springframework.http.HttpStatus.BAD_REQUEST, AiToolErrorCode.VALIDATION,
					"Solo puedes editar mensajes enviados por ti.");
		}
		final java.time.Instant anchorCreatedAt = anchor.getCreatedAt();
		final List<Long> discardedDraftIds = new ArrayList<>();
		for (final AiGeneratedDraft draft : draftRepository
			.findByThreadIdAndStatusAndCreatedAtGreaterThanEqual(threadId, AiDraftStatus.DRAFT, anchorCreatedAt)) {
			draft.setStatus(AiDraftStatus.DISCARDED);
			draftRepository.save(draft);
			discardedDraftIds.add(draft.getId());
		}
		final int truncatedMessageCount = chatPersistence.getMessageRepository()
			.deleteByThreadIdAndIdGreaterThanEqual(threadId, anchor.getId());
		auditLogger.logThreadTruncated(threadId, messageId, truncatedMessageCount, discardedDraftIds.size());
		return new TruncateOutcome(truncatedMessageCount, List.copyOf(discardedDraftIds));
	}

	private record TruncateOutcome(int truncatedMessageCount, List<Long> discardedDraftIds) {
	}

}
