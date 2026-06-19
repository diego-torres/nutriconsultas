package com.nutriconsultas.admin;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.nutriconsultas.subscription.Clinic;
import com.nutriconsultas.subscription.ClinicRepository;
import com.nutriconsultas.subscription.NutritionistInvitation;
import com.nutriconsultas.subscription.NutritionistInvitationRepository;

@Service
public class SubscriptionOwnerResolver {

	private final NutritionistInvitationRepository invitationRepository;

	private final ClinicRepository clinicRepository;

	public SubscriptionOwnerResolver(final NutritionistInvitationRepository invitationRepository,
			final ClinicRepository clinicRepository) {
		this.invitationRepository = invitationRepository;
		this.clinicRepository = clinicRepository;
	}

	public Optional<SubscriptionOwnerView> resolve(final Long subscriptionId) {
		if (subscriptionId == null) {
			return Optional.empty();
		}
		final Optional<NutritionistInvitation> invitationOpt = invitationRepository
			.findBySubscriptionId(subscriptionId);
		if (invitationOpt.isPresent()) {
			final NutritionistInvitation invitation = invitationOpt.get();
			return Optional.of(new SubscriptionOwnerView(invitation.getEmail(),
					resolveUserId(invitation, subscriptionId), invitation.getId()));
		}
		return clinicRepository.findBySubscriptionId(subscriptionId)
			.map(clinic -> new SubscriptionOwnerView(null, clinic.getDirectorUserId(), null));
	}

	private String resolveUserId(final NutritionistInvitation invitation, final Long subscriptionId) {
		if (StringUtils.hasText(invitation.getRedeemedByUserId())) {
			return invitation.getRedeemedByUserId();
		}
		return clinicRepository.findBySubscriptionId(subscriptionId).map(Clinic::getDirectorUserId).orElse(null);
	}

}
