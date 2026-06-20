package com.nutriconsultas.dieta;

import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.paciente.PacienteDietaRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Deletes diets when authorized. Owned diets require no {@code PacienteDieta} rows for
 * the nutritionist's patients. System template diets may be deleted by platform admins
 * only and are blocked when any patient assignment exists platform-wide.
 */
@Service
@Slf4j
public class DietaDeletionServiceImpl implements DietaDeletionService {

	private final DietaService dietaService;

	private final DietaAuthorization dietaAuthorization;

	private final PacienteDietaRepository pacienteDietaRepository;

	public DietaDeletionServiceImpl(final DietaService dietaService, final DietaAuthorization dietaAuthorization,
			final PacienteDietaRepository pacienteDietaRepository) {
		this.dietaService = dietaService;
		this.dietaAuthorization = dietaAuthorization;
		this.pacienteDietaRepository = pacienteDietaRepository;
	}

	@Override
	@Transactional
	public DietaDeleteResult deleteDieta(@NonNull final Long dietaId, @NonNull final String userId,
			final OidcUser principal) {
		final Dieta dieta = dietaAuthorization.resolveForMutation(dietaId, userId, principal, dietaService);
		if (dieta == null) {
			if (dietaService.getDieta(dietaId) == null) {
				return DietaDeleteResult.notFound();
			}
			return DietaDeleteResult.forbidden();
		}
		if (!dietaAuthorization.canModify(dieta, userId, principal)) {
			return DietaDeleteResult.forbidden();
		}

		final long assignedCount = countPatientAssignments(dieta, userId);
		if (assignedCount > 0) {
			if (log.isInfoEnabled()) {
				log.info("Blocked delete of diet {}: assigned to {} patient(s)", dietaId, assignedCount);
			}
			return DietaDeleteResult.inUse(assignedCount);
		}

		dietaAuthorization.auditSystemDietMutationIfNeeded(principal, dieta, "dietas.delete");
		dietaService.deleteDieta(dietaId);
		if (log.isInfoEnabled()) {
			log.info("Deleted diet {}", dietaId);
		}
		return DietaDeleteResult.deleted();
	}

	@Override
	@Transactional(readOnly = true)
	public long countPatientAssignments(@NonNull final Dieta dieta, @NonNull final String userId) {
		if (DietaCatalogConstants.isSystemTemplate(dieta)) {
			return pacienteDietaRepository.countByDietaId(dieta.getId());
		}
		return pacienteDietaRepository.countByDietaIdAndPacienteUserId(dieta.getId(), userId);
	}

}
