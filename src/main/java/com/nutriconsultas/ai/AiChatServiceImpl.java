package com.nutriconsultas.ai;

import java.util.ArrayList;
import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
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

	private final AiChatThreadRepository threadRepository;

	private final AiChatMessageRepository messageRepository;

	private final AiGeneratedDraftRepository draftRepository;

	private final AiOrchestrationService orchestrationService;

	private final AiPatientPromptContextResolver patientContextResolver;

	private final AiDietaPromptContextResolver dietaContextResolver;

	private final AiPlatilloPromptContextResolver platilloContextResolver;

	private final PacienteRepository pacienteRepository;

	private final TransactionTemplate transactionTemplate;

	public AiChatServiceImpl(final AiChatThreadRepository threadRepository,
			final AiChatMessageRepository messageRepository, final AiGeneratedDraftRepository draftRepository,
			final AiOrchestrationService orchestrationService,
			final AiPatientPromptContextResolver patientContextResolver,
			final AiDietaPromptContextResolver dietaContextResolver,
			final AiPlatilloPromptContextResolver platilloContextResolver,
			final PacienteRepository pacienteRepository, final TransactionTemplate transactionTemplate) {
		this.threadRepository = threadRepository;
		this.messageRepository = messageRepository;
		this.draftRepository = draftRepository;
		this.orchestrationService = orchestrationService;
		this.patientContextResolver = patientContextResolver;
		this.dietaContextResolver = dietaContextResolver;
		this.platilloContextResolver = platilloContextResolver;
		this.pacienteRepository = pacienteRepository;
		this.transactionTemplate = transactionTemplate;
	}

	@Override
	@Transactional
	public AiChatThread startThread(@NonNull final String nutritionistId, @Nullable final String title,
			@Nullable final Long patientId, @Nullable final Long clinicId, final AiChatPromptContext promptContext) {
		assertNutritionistId(nutritionistId);
		final AiChatPromptContext resolvedPromptContext = mergePromptContext(promptContext, null);
		final Long linkedPatientId = patientId != null ? patientId : resolvedPromptContext.patientId();
		final Paciente patient = resolveOwnedPatient(linkedPatientId, nutritionistId);
		final AiChatThread thread = new AiChatThread();
		thread.setNutritionistId(nutritionistId);
		thread.setTitle(resolveTitle(title));
		thread.setClinicId(clinicId);
		thread.setPatient(patient);
		final AiChatThread saved = threadRepository.save(thread);
		if (log.isInfoEnabled()) {
			log.info("AI chat thread created id={} patientLinked={}", saved.getId(), patient != null);
		}
		return saved;
	}

	@Override
	@Transactional(readOnly = true)
	public AiChatThreadDetail getThread(@NonNull final String nutritionistId, final long threadId) {
		assertNutritionistId(nutritionistId);
		final AiChatThread thread = loadOwnedThread(threadId, nutritionistId);
		final List<AiChatMessageView> messages = toVisibleMessages(
				messageRepository.findByThreadIdOrderByCreatedAtAscIdAsc(threadId));
		return new AiChatThreadDetail(thread.getId(), thread.getTitle(), patientId(thread), thread.getClinicId(),
				thread.getCreatedAt(), thread.getUpdatedAt(), messages);
	}

	@Override
	@Transactional(readOnly = true)
	public AiChatDraftList listDrafts(@NonNull final String nutritionistId, final long threadId) {
		assertNutritionistId(nutritionistId);
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
		assertNutritionistId(nutritionistId);
		if (!StringUtils.hasText(message)) {
			throw new AiChatException(org.springframework.http.HttpStatus.BAD_REQUEST, AiToolErrorCode.VALIDATION,
					"El mensaje no puede estar vacío.");
		}
		final AiChatThread thread = loadOwnedThread(threadId, nutritionistId);
		final AiChatPromptContext mergedContext = mergePromptContext(promptContext, patientId(thread));
		final AiOrchestrationContext context = buildOrchestrationContext(nutritionistId, threadId, mergedContext);
		return orchestrationService.processUserMessage(context, message.trim());
	}

	@Override
	public void streamMessage(@NonNull final String nutritionistId, final AiSendMessageRequest request,
			final SseEmitter emitter) {
		assertNutritionistId(nutritionistId);
		if (request == null || !StringUtils.hasText(request.message())) {
			completeStreamWithError(emitter, "El mensaje no puede estar vacío.");
			return;
		}
		try {
			final AiStreamCancellation cancellation = new AiStreamCancellation();
			emitter.onCompletion(cancellation::cancel);
			emitter.onTimeout(cancellation::cancel);
			emitter.onError(ex -> cancellation.cancel());
			final AiChatPromptContext promptContext = new AiChatPromptContext(request.patientId(), request.dietaId(),
					request.platilloId());
			final Long threadPatientId = transactionTemplate.execute(status -> {
				final AiChatThread ownedThread = loadOwnedThread(request.threadId(), nutritionistId);
				return patientId(ownedThread);
			});
			final AiChatPromptContext mergedContext = mergePromptContext(promptContext, threadPatientId);
			final AiOrchestrationContext context = buildOrchestrationContext(nutritionistId, request.threadId(),
					mergedContext);
			orchestrationService.processUserMessageStreaming(context, request.message().trim(),
					streamConsumerFor(emitter, cancellation));
			AiChatSseSupport.completeQuietly(emitter);
		}
		catch (final AiStreamCancelledException ex) {
			if (log.isDebugEnabled()) {
				log.debug("AI chat stream cancelled threadId={}", request.threadId());
			}
			AiChatSseSupport.completeQuietly(emitter);
		}
		catch (final AiChatException | AiOrchestrationException ex) {
			completeStreamWithError(emitter, ex.getMessage());
		}
		catch (final OpenAiClientException ex) {
			completeStreamWithError(emitter, ex.getUserMessage());
		}
		catch (final AiStreamDeliveryException ex) {
			if (log.isWarnEnabled()) {
				log.warn("AI chat stream delivery failed threadId={}", request.threadId());
			}
			AiChatSseSupport.completeQuietly(emitter);
		}
	}

	private AiStreamEventConsumer streamConsumerFor(final SseEmitter emitter,
			final AiStreamCancellation cancellation) {
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

	private static void completeStreamWithError(final SseEmitter emitter, final String message) {
		try {
			AiChatSseSupport.sendError(emitter, message);
		}
		catch (Exception ex) {
			emitter.completeWithError(ex);
			return;
		}
		AiChatSseSupport.completeQuietly(emitter);
	}

	private AiOrchestrationContext buildOrchestrationContext(final String nutritionistId, final long threadId,
			final AiChatPromptContext promptContext) {
		final AiPatientPromptContext patientContext = patientContextResolver
			.resolve(promptContext.patientId(), nutritionistId)
			.orElse(null);
		final AiDietaPromptContext dietaContext = dietaContextResolver.resolve(promptContext.dietaId(), nutritionistId)
			.orElse(null);
		final AiPlatilloPromptContext platilloContext = platilloContextResolver
			.resolve(promptContext.platilloId(), nutritionistId)
			.orElse(null);
		return new AiOrchestrationContext(nutritionistId, threadId, patientContext, dietaContext, platilloContext);
	}

	private static AiChatPromptContext mergePromptContext(final AiChatPromptContext requestContext,
			@Nullable final Long threadPatientId) {
		final AiChatPromptContext safeRequest = requestContext != null ? requestContext : AiChatPromptContext.empty();
		final Long patientId = safeRequest.patientId() != null ? safeRequest.patientId() : threadPatientId;
		return new AiChatPromptContext(patientId, safeRequest.dietaId(), safeRequest.platilloId());
	}

	private AiChatThread loadOwnedThread(final long threadId, final String nutritionistId) {
		return threadRepository.findByIdAndNutritionistId(threadId, nutritionistId)
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

	private static void assertNutritionistId(final String nutritionistId) {
		if (!StringUtils.hasText(nutritionistId)) {
			throw new AiChatException(org.springframework.http.HttpStatus.UNAUTHORIZED, AiToolErrorCode.VALIDATION,
					"Sesión no válida.");
		}
	}

}
