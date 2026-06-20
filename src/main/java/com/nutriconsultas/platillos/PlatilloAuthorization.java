package com.nutriconsultas.platillos;

import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import com.nutriconsultas.platform.PlatformAdminAuditService;
import com.nutriconsultas.platform.PlatformAdminService;

import lombok.extern.slf4j.Slf4j;

/**
 * Platillo mutation access: nutritionists may edit their own platillos; platform admins
 * may also edit system catalog rows
 * ({@link PlatilloCatalogConstants#SYSTEM_CATALOG_USER_ID}).
 */
@Component
@Slf4j
public class PlatilloAuthorization {

	private final PlatformAdminService platformAdminService;

	private final PlatformAdminAuditService platformAdminAuditService;

	public PlatilloAuthorization(final PlatformAdminService platformAdminService,
			final PlatformAdminAuditService platformAdminAuditService) {
		this.platformAdminService = platformAdminService;
		this.platformAdminAuditService = platformAdminAuditService;
	}

	public boolean canModify(final Platillo platillo, final String userId, final OidcUser principal) {
		if (platillo == null || userId == null) {
			return false;
		}
		return Objects.equals(userId, platillo.getUserId()) || (platformAdminService.isPlatformAdmin(principal)
				&& PlatilloCatalogConstants.isSystemCatalog(platillo));
	}

	public boolean canCopy(final Platillo platillo, final String userId) {
		if (platillo == null || userId == null) {
			return false;
		}
		return PlatilloCatalogConstants.isSystemCatalog(platillo) || Objects.equals(userId, platillo.getUserId());
	}

	public void verifyCanModify(final Platillo platillo, final String userId, final OidcUser principal) {
		if (platillo == null) {
			throw new IllegalArgumentException("Platillo no encontrado");
		}
		if (!canModify(platillo, userId, principal)) {
			if (log.isWarnEnabled()) {
				log.warn("User {} attempted to modify platillo {} owned by {}", userId, platillo.getId(),
						platillo.getUserId());
			}
			throw new IllegalArgumentException("No tiene permiso para modificar este platillo");
		}
	}

	public Platillo resolveForMutation(@NonNull final Long id, @NonNull final String userId, final OidcUser principal,
			final PlatilloService platilloService) {
		final Platillo owned = platilloService.findByIdAndUserId(id, userId);
		if (owned != null) {
			return owned;
		}
		if (!platformAdminService.isPlatformAdmin(principal)) {
			return null;
		}
		final Platillo platillo = platilloService.findById(id);
		if (platillo != null && PlatilloCatalogConstants.isSystemCatalog(platillo)) {
			return platillo;
		}
		return null;
	}

	public void auditSystemPlatilloMutationIfNeeded(final OidcUser principal, final Platillo platillo,
			@NonNull final String action) {
		if (platillo == null || !PlatilloCatalogConstants.isSystemCatalog(platillo)
				|| !platformAdminService.isPlatformAdmin(principal)) {
			return;
		}
		final String actorUserId = platformAdminService.resolveActorUserId(principal);
		platformAdminAuditService.recordAction(actorUserId, action + ":platilloId=" + platillo.getId());
	}

}
