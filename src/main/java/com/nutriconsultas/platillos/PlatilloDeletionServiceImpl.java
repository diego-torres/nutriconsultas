package com.nutriconsultas.platillos;

import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.dieta.PlatilloIngestaRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Deletes platillos when authorized. Owned platillos are blocked when referenced in the
 * nutritionist's diet templates. System catalog platillos may be deleted by platform
 * admins only and are blocked when referenced in any diet template.
 */
@Service
@Slf4j
public class PlatilloDeletionServiceImpl implements PlatilloDeletionService {

	private final PlatilloService platilloService;

	private final PlatilloAuthorization platilloAuthorization;

	private final PlatilloIngestaRepository platilloIngestaRepository;

	public PlatilloDeletionServiceImpl(final PlatilloService platilloService,
			final PlatilloAuthorization platilloAuthorization,
			final PlatilloIngestaRepository platilloIngestaRepository) {
		this.platilloService = platilloService;
		this.platilloAuthorization = platilloAuthorization;
		this.platilloIngestaRepository = platilloIngestaRepository;
	}

	@Override
	@Transactional
	public PlatilloDeleteResult deletePlatillo(@NonNull final Long platilloId, @NonNull final String userId,
			final OidcUser principal) {
		final Platillo platillo = platilloAuthorization.resolveForMutation(platilloId, userId, principal,
				platilloService);
		if (platillo == null) {
			if (platilloService.findById(platilloId) == null) {
				return PlatilloDeleteResult.notFound();
			}
			return PlatilloDeleteResult.forbidden();
		}
		if (!platilloAuthorization.canModify(platillo, userId, principal)) {
			return PlatilloDeleteResult.forbidden();
		}

		final long referenceCount = countDietReferences(platillo, userId);
		if (referenceCount > 0) {
			if (log.isInfoEnabled()) {
				log.info("Blocked delete of platillo {}: referenced in {} diet template(s)", platilloId,
						referenceCount);
			}
			return PlatilloDeleteResult.inUse(referenceCount);
		}

		platilloAuthorization.auditSystemPlatilloMutationIfNeeded(principal, platillo, "platillos.delete");
		platilloService.deletePlatillo(platilloId);
		if (log.isInfoEnabled()) {
			log.info("Deleted platillo {}", platilloId);
		}
		return PlatilloDeleteResult.deleted();
	}

	@Override
	@Transactional(readOnly = true)
	public long countDietReferences(@NonNull final Platillo platillo, @NonNull final String userId) {
		if (PlatilloCatalogConstants.isSystemCatalog(platillo)) {
			return platilloIngestaRepository.countBySourcePlatilloId(platillo.getId());
		}
		return platilloIngestaRepository.countBySourcePlatilloIdAndDietaUserId(platillo.getId(), userId);
	}

}
