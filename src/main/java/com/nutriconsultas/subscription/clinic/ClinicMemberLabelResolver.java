package com.nutriconsultas.subscription.clinic;

import java.util.Optional;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.nutriconsultas.auth0.Auth0UserLookup;
import com.nutriconsultas.profile.NutritionistProfile;
import com.nutriconsultas.profile.NutritionistProfileRepository;
import com.nutriconsultas.subscription.ClinicInvitation;
import com.nutriconsultas.subscription.ClinicInvitationRepository;
import com.nutriconsultas.subscription.NutritionistInvitation;
import com.nutriconsultas.subscription.NutritionistInvitationRepository;

/**
 * Resolves clinic roster labels: profile display name, else invitation or Auth0 email.
 */
@Service
public class ClinicMemberLabelResolver {

	private final NutritionistProfileRepository nutritionistProfileRepository;

	private final NutritionistInvitationRepository nutritionistInvitationRepository;

	private final ClinicInvitationRepository clinicInvitationRepository;

	private final Auth0UserLookup auth0UserLookup;

	public ClinicMemberLabelResolver(final NutritionistProfileRepository nutritionistProfileRepository,
			final NutritionistInvitationRepository nutritionistInvitationRepository,
			final ClinicInvitationRepository clinicInvitationRepository, final Auth0UserLookup auth0UserLookup) {
		this.nutritionistProfileRepository = nutritionistProfileRepository;
		this.nutritionistInvitationRepository = nutritionistInvitationRepository;
		this.clinicInvitationRepository = clinicInvitationRepository;
		this.auth0UserLookup = auth0UserLookup;
	}

	public String resolveLabel(@NonNull final String userId) {
		if (!StringUtils.hasText(userId)) {
			return "Correo no disponible";
		}
		final Optional<NutritionistProfile> profile = nutritionistProfileRepository.findByUserId(userId);
		if (profile.isPresent() && StringUtils.hasText(profile.get().getDisplayName())) {
			return profile.get().getDisplayName().trim();
		}
		final Optional<String> invitationEmail = resolveInvitationEmail(userId);
		if (invitationEmail.isPresent()) {
			return invitationEmail.get();
		}
		return auth0UserLookup.findEmailByUserId(userId).orElse("Correo no disponible");
	}

	private Optional<String> resolveInvitationEmail(final String userId) {
		final Optional<NutritionistInvitation> nutritionistInvitation = nutritionistInvitationRepository
			.findFirstByRedeemedByUserIdOrderByRedeemedAtDesc(userId);
		if (nutritionistInvitation.isPresent() && StringUtils.hasText(nutritionistInvitation.get().getEmail())) {
			return Optional.of(nutritionistInvitation.get().getEmail().trim());
		}
		final Optional<ClinicInvitation> clinicInvitation = clinicInvitationRepository
			.findFirstByRedeemedByUserIdOrderByRedeemedAtDesc(userId);
		if (clinicInvitation.isPresent() && StringUtils.hasText(clinicInvitation.get().getEmail())) {
			return Optional.of(clinicInvitation.get().getEmail().trim());
		}
		return Optional.empty();
	}

}
