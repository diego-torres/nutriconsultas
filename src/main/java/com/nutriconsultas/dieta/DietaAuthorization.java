package com.nutriconsultas.dieta;

import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.platform.PlatformAdminAuditService;
import com.nutriconsultas.platform.PlatformAdminService;

import lombok.extern.slf4j.Slf4j;

/**
 * Diet mutation access: nutritionists may edit their own diets; platform admins may also
 * edit system template diets ({@link DietaCatalogConstants#SYSTEM_TEMPLATE_USER_ID}).
 * Patient-assigned copies are editable by the nutritionist who owns the patient.
 */
@Component
@Slf4j
public class DietaAuthorization {

	private final PlatformAdminService platformAdminService;

	private final PlatformAdminAuditService platformAdminAuditService;

	private final PacienteRepository pacienteRepository;

	public DietaAuthorization(final PlatformAdminService platformAdminService,
			final PlatformAdminAuditService platformAdminAuditService, final PacienteRepository pacienteRepository) {
		this.platformAdminService = platformAdminService;
		this.platformAdminAuditService = platformAdminAuditService;
		this.pacienteRepository = pacienteRepository;
	}

	public boolean canModify(final Dieta dieta, final String userId, final OidcUser principal) {
		if (dieta == null || userId == null) {
			return false;
		}
		if (DietaCatalogConstants.isPatientAssignment(dieta)) {
			return ownsPatientDieta(dieta, userId);
		}
		return java.util.Objects.equals(userId, dieta.getUserId())
				|| (platformAdminService.isPlatformAdmin(principal) && DietaCatalogConstants.isSystemTemplate(dieta));
	}

	public boolean canView(final Dieta dieta, final String userId, final OidcUser principal) {
		if (dieta == null || userId == null) {
			return false;
		}
		return !DietaCatalogConstants.isPatientAssignment(dieta) || ownsPatientDieta(dieta, userId);
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

	public void verifyCanView(final Dieta dieta, final String userId, final OidcUser principal) {
		if (dieta == null) {
			throw new IllegalArgumentException("Dieta no encontrada");
		}
		if (!canView(dieta, userId, principal)) {
			throw new IllegalArgumentException("No tiene permiso para ver esta dieta");
		}
	}

	public Dieta resolveForMutation(@NonNull final Long id, @NonNull final String userId, final OidcUser principal,
			final DietaService dietaService) {
		final Dieta dieta = dietaService.getDieta(id);
		if (dieta == null) {
			return null;
		}
		if (DietaCatalogConstants.isPatientAssignment(dieta)) {
			return ownsPatientDieta(dieta, userId) ? dieta : null;
		}
		final Dieta owned = dietaService.getDietaByIdAndUserId(id, userId);
		if (owned != null) {
			return owned;
		}
		if (!platformAdminService.isPlatformAdmin(principal)) {
			return null;
		}
		if (DietaCatalogConstants.isSystemTemplate(dieta)) {
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

	private boolean ownsPatientDieta(final Dieta dieta, final String userId) {
		return dieta.getPacienteId() != null
				&& pacienteRepository.findByIdAndUserId(dieta.getPacienteId(), userId).isPresent();
	}

}
