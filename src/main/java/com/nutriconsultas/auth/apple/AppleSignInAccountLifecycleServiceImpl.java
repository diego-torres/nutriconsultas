package com.nutriconsultas.auth.apple;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nutriconsultas.auth0.Auth0ManagementApiException;
import com.nutriconsultas.auth0.Auth0ManagementUserService;
import com.nutriconsultas.paciente.ApplePacienteLifecycleStatus;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.PacienteStatus;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AppleSignInAccountLifecycleServiceImpl implements AppleSignInAccountLifecycleService {

	private final PacienteRepository pacienteRepository;

	private final Auth0ManagementUserService auth0ManagementUserService;

	public AppleSignInAccountLifecycleServiceImpl(final PacienteRepository pacienteRepository,
			final Auth0ManagementUserService auth0ManagementUserService) {
		this.pacienteRepository = pacienteRepository;
		this.auth0ManagementUserService = auth0ManagementUserService;
	}

	@Override
	@Transactional
	public AppleSignInLifecycleAction applyDestructiveEvent(final AppleSignInNotification notification,
			final AppleSignInEventType eventType) {
		if (!eventType.isDestructive()) {
			return AppleSignInLifecycleAction.NOT_APPLICABLE;
		}
		if (notification.getPacienteId() == null) {
			return AppleSignInLifecycleAction.SKIPPED_NO_PACIENTE;
		}
		final Optional<Paciente> pacienteOptional = pacienteRepository.findById(notification.getPacienteId());
		if (pacienteOptional.isEmpty()) {
			return AppleSignInLifecycleAction.SKIPPED_NO_PACIENTE;
		}
		final Paciente paciente = pacienteOptional.get();
		final ApplePacienteLifecycleStatus targetStatus = targetLifecycleStatus(eventType);
		if (paciente.getAppleLifecycleStatus() == targetStatus) {
			return AppleSignInLifecycleAction.ALREADY_APPLIED;
		}
		paciente.setAppleLifecycleStatus(targetStatus);
		if (eventType == AppleSignInEventType.CONSENT_REVOKED && paciente.getStatus() != PacienteStatus.REVOKED) {
			paciente.setStatus(PacienteStatus.REVOKED);
		}
		pacienteRepository.save(paciente);
		try {
			updateAuth0LifecycleMetadata(notification, eventType, targetStatus);
		}
		catch (Auth0ManagementApiException ex) {
			if (log.isWarnEnabled()) {
				log.warn("Auth0 lifecycle metadata update failed for pacienteId={}", notification.getPacienteId());
			}
			return AppleSignInLifecycleAction.AUTH0_UPDATE_FAILED;
		}
		if (log.isInfoEnabled()) {
			log.info("Applied Apple lifecycle status={} for pacienteId={} eventType={}", targetStatus, paciente.getId(),
					eventType);
		}
		return lifecycleActionFor(eventType);
	}

	private void updateAuth0LifecycleMetadata(final AppleSignInNotification notification,
			final AppleSignInEventType eventType, final ApplePacienteLifecycleStatus targetStatus) {
		if (!StringUtils.hasText(notification.getAuth0UserId()) || !auth0ManagementUserService.isConfigured()) {
			return;
		}
		auth0ManagementUserService.updateAppMetadata(notification.getAuth0UserId(),
				Map.of("apple_signin_lifecycle_status", targetStatus.name(), "apple_signin_lifecycle_at",
						Instant.now().toString(), "apple_signin_last_event_type", eventType.name(),
						"apple_signin_last_event_id", notification.getAppleEventId()));
		if (eventType == AppleSignInEventType.CONSENT_REVOKED) {
			auth0ManagementUserService.blockUserInAppMetadata(notification.getAuth0UserId());
		}
	}

	private static ApplePacienteLifecycleStatus targetLifecycleStatus(final AppleSignInEventType eventType) {
		if (eventType == AppleSignInEventType.ACCOUNT_DELETE) {
			return ApplePacienteLifecycleStatus.PENDING_DELETION_REVIEW;
		}
		return ApplePacienteLifecycleStatus.ACCESS_REVOKED;
	}

	private static AppleSignInLifecycleAction lifecycleActionFor(final AppleSignInEventType eventType) {
		if (eventType == AppleSignInEventType.ACCOUNT_DELETE) {
			return AppleSignInLifecycleAction.APPLIED_PENDING_DELETION_REVIEW;
		}
		return AppleSignInLifecycleAction.APPLIED_ACCESS_REVOKED;
	}

}
