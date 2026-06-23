package com.nutriconsultas.paciente.invitation;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.mobile.PatientInvitationInvalidTokenException;
import com.nutriconsultas.mobile.PatientInvitationUnavailableException;
import com.nutriconsultas.paciente.PatientInvitation;
import com.nutriconsultas.paciente.PatientInvitationRepository;
import com.nutriconsultas.paciente.PatientInvitationStatus;
import com.nutriconsultas.profile.NutritionistBrandingHelper;
import com.nutriconsultas.profile.NutritionistProfileRepository;
import com.nutriconsultas.util.InvitationTokenHasher;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PatientInvitationPreviewServiceImpl implements PatientInvitationPreviewService {

	private final PatientInvitationRepository patientInvitationRepository;

	private final NutritionistProfileRepository nutritionistProfileRepository;

	public PatientInvitationPreviewServiceImpl(final PatientInvitationRepository patientInvitationRepository,
			final NutritionistProfileRepository nutritionistProfileRepository) {
		this.patientInvitationRepository = patientInvitationRepository;
		this.nutritionistProfileRepository = nutritionistProfileRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public PatientInvitationPreviewResult preview(final String rawUrlToken) {
		if (!PatientInvitationUrlTokens.isWellFormed(rawUrlToken)) {
			throw new PatientInvitationInvalidTokenException();
		}
		final String tokenHash = InvitationTokenHasher.hashToken(rawUrlToken);
		final PatientInvitation invitation = patientInvitationRepository.findByTokenHash(tokenHash)
			.filter(this::isPreviewable)
			.orElseThrow(PatientInvitationUnavailableException::new);

		final String inviterDisplayName = nutritionistProfileRepository.findByUserId(invitation.getNutritionistUserId())
			.map(profile -> NutritionistBrandingHelper.resolveDisplayName(profile, null))
			.orElse(null);

		if (log.isDebugEnabled()) {
			log.debug("Patient invitation preview resolved for invitationId={}", invitation.getId());
		}

		return new PatientInvitationPreviewResult(inviterDisplayName);
	}

	private boolean isPreviewable(final PatientInvitation invitation) {
		if (invitation.getStatus() != PatientInvitationStatus.PENDING) {
			return false;
		}
		return !invitation.getExpiresAt().isBefore(Instant.now());
	}

}
