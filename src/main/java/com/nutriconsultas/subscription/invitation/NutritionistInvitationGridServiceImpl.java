package com.nutriconsultas.subscription.invitation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.subscription.NutritionistInvitation;
import com.nutriconsultas.subscription.NutritionistInvitationRepository;

@Service
public class NutritionistInvitationGridServiceImpl implements NutritionistInvitationGridService {

	private final NutritionistInvitationRepository invitationRepository;

	public NutritionistInvitationGridServiceImpl(final NutritionistInvitationRepository invitationRepository) {
		this.invitationRepository = invitationRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public Page<NutritionistInvitation> findPage(final NutritionistInvitationGridFilters filters,
			final Pageable pageable) {
		return invitationRepository.findAll(specification(filters), pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public long countFiltered(final NutritionistInvitationGridFilters filters) {
		return invitationRepository.count(specification(filters));
	}

	@Override
	@Transactional(readOnly = true)
	public long countAll() {
		return invitationRepository.count();
	}

	private static Specification<NutritionistInvitation> specification(
			final NutritionistInvitationGridFilters filters) {
		return NutritionistInvitationSpecifications.withFilters(filters);
	}

}
