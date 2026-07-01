package com.nutriconsultas.ai;

import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nutriconsultas.dieta.Dieta;
import com.nutriconsultas.platillos.Platillo;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AiDraftAcceptanceServiceImpl implements AiDraftAcceptanceService {

	private final AiGeneratedDraftRepository draftRepository;

	private final AiDraftMaterializationService materializationService;

	private final AiDraftLifecycleService draftLifecycleService;

	public AiDraftAcceptanceServiceImpl(final AiGeneratedDraftRepository draftRepository,
			final AiDraftMaterializationService materializationService,
			final AiDraftLifecycleService draftLifecycleService) {
		this.draftRepository = draftRepository;
		this.materializationService = materializationService;
		this.draftLifecycleService = draftLifecycleService;
	}

	@Override
	@Transactional
	public AiDraftAcceptanceResult accept(@NonNull final Long draftId, @NonNull final String nutritionistId,
			@NonNull final OidcUser principal) {
		if (!StringUtils.hasText(nutritionistId)) {
			throw new AiDraftLifecycleException("Sesión de nutriólogo no válida.");
		}
		final AiGeneratedDraft draft = loadMutableDraft(draftId, nutritionistId);
		final MaterializedEntity entity = materialize(draft, nutritionistId, principal);
		final AiGeneratedDraft accepted = draftLifecycleService.acceptDraft(draftId, nutritionistId);
		final String summary = buildSummary(draft.getDraftType(), entity);
		if (log.isInfoEnabled()) {
			log.info("AI draft accepted id={} status={} createdEntityType={} createdEntityId={}", accepted.getId(),
					accepted.getStatus(), entity.entityType(), entity.entityId());
		}
		return new AiDraftAcceptanceResult(accepted.getId(), accepted.getDraftType(), accepted.getStatus(),
				entity.entityType(), entity.entityId(), summary);
	}

	private AiGeneratedDraft loadMutableDraft(final Long draftId, final String nutritionistId) {
		final AiGeneratedDraft draft = draftRepository.findByIdAndThreadNutritionistId(draftId, nutritionistId)
			.orElseThrow(() -> new AiDraftLifecycleException("Borrador no encontrado."));
		if (draft.getStatus() != AiDraftStatus.DRAFT) {
			throw new AiDraftLifecycleException("El borrador ya no se puede modificar.");
		}
		return draft;
	}

	private MaterializedEntity materialize(final AiGeneratedDraft draft, final String nutritionistId,
			final OidcUser principal) {
		return switch (draft.getDraftType()) {
			case DISH -> materializeDish(draft, nutritionistId, principal);
			case MENU -> materializeMenu(draft, nutritionistId, principal);
			case DIET_PLAN -> materializeDietPlan(draft, nutritionistId, principal);
		};
	}

	private MaterializedEntity materializeDish(final AiGeneratedDraft draft, final String nutritionistId,
			final OidcUser principal) {
		final DishDraftPayload payload = AiDraftPayloadDeserializer.dish(draft.getJsonPayload());
		final Platillo platillo = materializationService.materializeDish(payload, nutritionistId, principal);
		return new MaterializedEntity(AiDraftCreatedEntityType.PLATILLO, platillo.getId());
	}

	private MaterializedEntity materializeMenu(final AiGeneratedDraft draft, final String nutritionistId,
			final OidcUser principal) {
		final MenuDraftPayload payload = AiDraftPayloadDeserializer.menu(draft.getJsonPayload());
		final Dieta dieta = materializationService.materializeMenu(payload, nutritionistId, principal);
		return new MaterializedEntity(AiDraftCreatedEntityType.DIETA, dieta.getId());
	}

	private MaterializedEntity materializeDietPlan(final AiGeneratedDraft draft, final String nutritionistId,
			final OidcUser principal) {
		final DietPlanDraftPayload payload = AiDraftPayloadDeserializer.dietPlan(draft.getJsonPayload());
		final Dieta dieta = materializationService.materializeDietPlan(payload, nutritionistId, principal);
		return new MaterializedEntity(AiDraftCreatedEntityType.DIETA, dieta.getId());
	}

	private static String buildSummary(final AiDraftType draftType, final MaterializedEntity entity) {
		return switch (draftType) {
			case DISH -> "Platillo creado en catálogo (id=" + entity.entityId() + ").";
			case MENU -> "Menú guardado como dieta en catálogo (id=" + entity.entityId() + ").";
			case DIET_PLAN -> "Plan alimenticio guardado en catálogo (id=" + entity.entityId() + ").";
		};
	}

	private record MaterializedEntity(AiDraftCreatedEntityType entityType, long entityId) {
	}

}
