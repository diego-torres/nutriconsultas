package com.nutriconsultas.ai;

import java.util.ArrayList;
import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

	private final PacienteRepository pacienteRepository;

	public AiChatServiceImpl(final AiChatThreadRepository threadRepository,
			final AiChatMessageRepository messageRepository, final AiGeneratedDraftRepository draftRepository,
			final AiOrchestrationService orchestrationService,
			final AiPatientPromptContextResolver patientContextResolver, final PacienteRepository pacienteRepository) {
		this.threadRepository = threadRepository;
		this.messageRepository = messageRepository;
		this.draftRepository = draftRepository;
		this.orchestrationService = orchestrationService;
		this.patientContextResolver = patientContextResolver;
		this.pacienteRepository = pacienteRepository;
	}

	@Override
	@Transactional
	public AiChatThread startThread(@NonNull final String nutritionistId, @Nullable final String title,
			@Nullable final Long patientId, @Nullable final Long clinicId) {
		assertNutritionistId(nutritionistId);
		final Paciente patient = resolveOwnedPatient(patientId, nutritionistId);
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
			final String message) {
		assertNutritionistId(nutritionistId);
		if (!StringUtils.hasText(message)) {
			throw new AiChatException(org.springframework.http.HttpStatus.BAD_REQUEST, AiToolErrorCode.VALIDATION,
					"El mensaje no puede estar vacío.");
		}
		final AiChatThread thread = loadOwnedThread(threadId, nutritionistId);
		final AiPatientPromptContext patientContext = patientContextResolver.resolve(patientId(thread), nutritionistId)
			.orElse(null);
		final AiOrchestrationContext context = new AiOrchestrationContext(nutritionistId, threadId, patientContext);
		return orchestrationService.processUserMessage(context, message.trim());
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
