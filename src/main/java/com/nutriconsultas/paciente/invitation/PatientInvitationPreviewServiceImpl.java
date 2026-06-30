package com.nutriconsultas.paciente.invitation;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nutriconsultas.mobile.PatientInvitationInvalidTokenException;
import com.nutriconsultas.mobile.PatientInvitationUnavailableException;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteStatus;
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

	private final PatientInvitationProperties invitationProperties;

	public PatientInvitationPreviewServiceImpl(final PatientInvitationRepository patientInvitationRepository,
			final NutritionistProfileRepository nutritionistProfileRepository,
			final PatientInvitationProperties invitationProperties) {
		this.patientInvitationRepository = patientInvitationRepository;
		this.nutritionistProfileRepository = nutritionistProfileRepository;
		this.invitationProperties = invitationProperties;
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

		return toPreviewResult(invitation);
	}

	@Override
	@Transactional(readOnly = true)
	public PatientInvitationPreviewResult previewByHumanCode(final String humanCode) {
		if (!PatientInvitationHumanCodes.isWellFormed(humanCode, invitationProperties.getHumanCodePrefix())) {
			throw new PatientInvitationUnavailableException();
		}
		final String normalizedCode = PatientInvitationHumanCodes.normalize(humanCode);
		final PatientInvitation invitation = patientInvitationRepository.findByHumanCode(normalizedCode)
			.filter(this::isPreviewable)
			.orElseThrow(PatientInvitationUnavailableException::new);

		return toPreviewResult(invitation);
	}

	private PatientInvitationPreviewResult toPreviewResult(final PatientInvitation invitation) {
		final String inviterDisplayName = nutritionistProfileRepository.findByUserId(invitation.getNutritionistUserId())
			.map(profile -> NutritionistBrandingHelper.resolveDisplayName(profile, null))
			.orElse(null);
		final Paciente paciente = invitation.getPaciente();
		final PacienteStatus patientStatus = paciente.getStatus();
		final boolean mobileAppLinked = PatientInvitationAuthRouting.isMobileAppLinked(paciente);
		final InvitationAuthPath authPath = PatientInvitationAuthRouting.resolveAuthPath(patientStatus,
				mobileAppLinked);
		final String emailHint = PatientInvitationAuthRouting.resolveEmailHint(paciente);

		if (log.isDebugEnabled()) {
			log.debug("Patient invitation preview resolved for invitationId={}", invitation.getId());
		}

		return new PatientInvitationPreviewResult(inviterDisplayName, patientStatus, mobileAppLinked, authPath,
				emailHint);
	}

	private boolean isPreviewable(final PatientInvitation invitation) {
		return invitation.getStatus() == PatientInvitationStatus.PENDING
				&& !invitation.getExpiresAt().isBefore(Instant.now());
	}

}
