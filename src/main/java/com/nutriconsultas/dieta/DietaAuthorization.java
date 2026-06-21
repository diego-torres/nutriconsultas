package com.nutriconsultas.dieta;

import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import com.nutriconsultas.platform.PlatformAdminAuditService;
import com.nutriconsultas.platform.PlatformAdminService;

import lombok.extern.slf4j.Slf4j;

/**
 * Diet mutation access: nutritionists may edit their own diets; platform admins may also
 * edit system template diets ({@link DietaCatalogConstants#SYSTEM_TEMPLATE_USER_ID}).
 */
@Component
@Slf4j
public class DietaAuthorization {

	private final PlatformAdminService platformAdminService;

	private final PlatformAdminAuditService platformAdminAuditService;

	public DietaAuthorization(final PlatformAdminService platformAdminService,
			final PlatformAdminAuditService platformAdminAuditService) {
		this.platformAdminService = platformAdminService;
		this.platformAdminAuditService = platformAdminAuditService;
	}

	public boolean canModify(final Dieta dieta, final String userId, final OidcUser principal) {
		if (dieta == null || userId == null) {
			return false;
		}
		return Objects.equals(userId, dieta.getUserId())
				|| (platformAdminService.isPlatformAdmin(principal) && DietaCatalogConstants.isSystemTemplate(dieta));
	}

	public void verifyCanModify(final Dieta dieta, final String userId, final OidcUser principal) {
		if (dieta == null) {
			throw new IllegalArgumentException("Dieta no encontrada");
		}
		if (!canModify(dieta, userId, principal)) {
			if (log.isWarnEnabled()) {
				log.warn("User {} attempted to modify diet {} owned by {}", userId, dieta.getId(), dieta.getUserId());
			}
			throw new IllegalArgumentException("No tiene permiso para modificar esta dieta");
		}
	}

	public Dieta resolveForMutation(@NonNull final Long id, @NonNull final String userId, final OidcUser principal,
			final DietaService dietaService) {
		final Dieta owned = dietaService.getDietaByIdAndUserId(id, userId);
		if (owned != null) {
			return owned;
		}
		if (!platformAdminService.isPlatformAdmin(principal)) {
			return null;
		}
		final Dieta dieta = dietaService.getDieta(id);
		if (dieta != null && DietaCatalogConstants.isSystemTemplate(dieta)) {
			return dieta;
		}
		return null;
	}

	public void auditSystemDietMutationIfNeeded(final OidcUser principal, final Dieta dieta,
			@NonNull final String action) {
		if (dieta == null || !DietaCatalogConstants.isSystemTemplate(dieta)
				|| !platformAdminService.isPlatformAdmin(principal)) {
			return;
		}
		final String actorUserId = platformAdminService.resolveActorUserId(principal);
		platformAdminAuditService.recordAction(actorUserId, action + ":dietaId=" + dieta.getId());
	}

	/**
	 * Resolves the owner {@code userId} for a newly created diet. Platform admins create
	 * system template rows; all other users create owned rows. Client-supplied
	 * {@code userId} values are ignored — callers must use this method instead of
	 * trusting request data.
	 */
	public String resolveCreateUserId(final OidcUser principal, @NonNull final String oauthUserId) {
		if (platformAdminService.isPlatformAdmin(principal)) {
			return DietaCatalogConstants.SYSTEM_TEMPLATE_USER_ID;
		}
		return oauthUserId;
	}

}
