package com.nutriconsultas.ai;

import java.time.Instant;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AiDraftLifecycleServiceImpl implements AiDraftLifecycleService {

	private final AiChatThreadRepository threadRepository;

	private final AiGeneratedDraftRepository draftRepository;

	private final AiEntitlementGuard aiEntitlementGuard;

	public AiDraftLifecycleServiceImpl(final AiChatThreadRepository threadRepository,
			final AiGeneratedDraftRepository draftRepository, final AiEntitlementGuard aiEntitlementGuard) {
		this.threadRepository = threadRepository;
		this.draftRepository = draftRepository;
		this.aiEntitlementGuard = aiEntitlementGuard;
	}

	@Override
	@Transactional
	public AiGeneratedDraft createDraft(@NonNull final Long threadId, @NonNull final String nutritionistId,
			@NonNull final AiDraftType draftType, @NonNull final String jsonPayload) {
		final AiChatThread thread = loadOwnedThread(threadId, nutritionistId);
		if (!StringUtils.hasText(jsonPayload)) {
			throw new AiDraftLifecycleException("El borrador no tiene contenido.");
		}
		final AiGeneratedDraft draft = new AiGeneratedDraft();
		draft.setThread(thread);
		draft.setDraftType(draftType);
		draft.setJsonPayload(jsonPayload.trim());
		draft.setStatus(AiDraftStatus.DRAFT);
		final AiGeneratedDraft saved = draftRepository.save(draft);
		if (log.isInfoEnabled()) {
			log.info("AI draft created id={} threadId={} type={}", saved.getId(), threadId, draftType);
		}
		return saved;
	}

	@Override
	@Transactional
	public AiGeneratedDraft acceptDraft(@NonNull final Long draftId, @NonNull final String nutritionistId) {
		aiEntitlementGuard.assertCanUseAiAssistant(nutritionistId);
		final AiGeneratedDraft draft = loadMutableDraft(draftId, nutritionistId);
		draft.setStatus(AiDraftStatus.ACCEPTED);
		draft.setAcceptedAt(Instant.now());
		final AiGeneratedDraft saved = draftRepository.save(draft);
		if (log.isInfoEnabled()) {
			log.info("AI draft accepted id={} threadId={}", saved.getId(), saved.getThread().getId());
		}
		return saved;
	}

	@Override
	@Transactional
	public AiGeneratedDraft discardDraft(@NonNull final Long draftId, @NonNull final String nutritionistId) {
		aiEntitlementGuard.assertCanUseAiAssistant(nutritionistId);
		final AiGeneratedDraft draft = loadMutableDraft(draftId, nutritionistId);
		draft.setStatus(AiDraftStatus.DISCARDED);
		final AiGeneratedDraft saved = draftRepository.save(draft);
		if (log.isInfoEnabled()) {
			log.info("AI draft discarded id={} threadId={}", saved.getId(), saved.getThread().getId());
		}
		return saved;
	}

	private AiChatThread loadOwnedThread(final Long threadId, final String nutritionistId) {
		return threadRepository.findByIdAndNutritionistId(threadId, nutritionistId)
			.orElseThrow(() -> new AiDraftLifecycleException("Conversación no encontrada."));
	}

	private AiGeneratedDraft loadMutableDraft(final Long draftId, final String nutritionistId) {
		final AiGeneratedDraft draft = draftRepository.findByIdAndThreadNutritionistId(draftId, nutritionistId)
			.orElseThrow(() -> new AiDraftLifecycleException("Borrador no encontrado."));
		if (draft.getStatus() != AiDraftStatus.DRAFT) {
			throw new AiDraftLifecycleException("El borrador ya no se puede modificar.");
		}
		return draft;
	}

}
