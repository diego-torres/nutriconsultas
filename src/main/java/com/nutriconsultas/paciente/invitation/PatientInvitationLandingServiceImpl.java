package com.nutriconsultas.paciente.invitation;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.nutriconsultas.mobile.PatientInvitationInvalidTokenException;
import com.nutriconsultas.mobile.PatientInvitationPreviewRateLimiter;
import com.nutriconsultas.mobile.PatientInvitationUnavailableException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PatientInvitationLandingServiceImpl implements PatientInvitationLandingService {

	private final PatientInvitationPreviewService patientInvitationPreviewService;

	private final PatientInvitationPreviewRateLimiter patientInvitationPreviewRateLimiter;

	private final PatientInvitationProperties patientInvitationProperties;

	public PatientInvitationLandingServiceImpl(final PatientInvitationPreviewService patientInvitationPreviewService,
			final PatientInvitationPreviewRateLimiter patientInvitationPreviewRateLimiter,
			final PatientInvitationProperties patientInvitationProperties) {
		this.patientInvitationPreviewService = patientInvitationPreviewService;
		this.patientInvitationPreviewRateLimiter = patientInvitationPreviewRateLimiter;
		this.patientInvitationProperties = patientInvitationProperties;
	}

	@Override
	public Optional<PatientInvitationLandingContent> resolve(final String rawUrlToken, final String clientKey) {
		try {
			final PatientInvitationLandingContent content = patientInvitationPreviewRateLimiter.execute(clientKey,
					() -> buildContent(rawUrlToken));
			return Optional.of(content);
		}
		catch (PatientInvitationInvalidTokenException | PatientInvitationUnavailableException ex) {
			logUnavailable();
			return Optional.empty();
		}
		catch (IllegalStateException ex) {
			if (isInvitationUnavailable(ex)) {
				logUnavailable();
				return Optional.empty();
			}
			throw ex;
		}
	}

	private static boolean isInvitationUnavailable(final Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			if (current instanceof PatientInvitationInvalidTokenException
					|| current instanceof PatientInvitationUnavailableException) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}

	private void logUnavailable() {
		if (log.isDebugEnabled()) {
			log.debug("Patient invitation landing unavailable");
		}
	}

	private PatientInvitationLandingContent buildContent(final String rawUrlToken) {
		final PatientInvitationPreviewResult preview = patientInvitationPreviewService.preview(rawUrlToken);
		final String humanCode = PatientInvitationHumanCode.fromUrlToken(rawUrlToken,
				patientInvitationProperties.getHumanCodePrefix());
		final String inviteUrl = patientInvitationProperties.buildInviteUrl(rawUrlToken);
		return new PatientInvitationLandingContent(preview.inviterDisplayName(), humanCode, inviteUrl);
	}

}
