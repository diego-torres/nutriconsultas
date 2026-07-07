package com.nutriconsultas.auth.apple;

import java.time.Instant;
import java.util.HashMap;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nutriconsultas.auth0.Auth0ManagementApiException;
import com.nutriconsultas.auth0.Auth0ManagementUserService;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AppleSignInRelayEmailServiceImpl implements AppleSignInRelayEmailService {

	private final PacienteRepository pacienteRepository;

	private final Auth0ManagementUserService auth0ManagementUserService;

	public AppleSignInRelayEmailServiceImpl(final PacienteRepository pacienteRepository,
			final Auth0ManagementUserService auth0ManagementUserService) {
		this.pacienteRepository = pacienteRepository;
		this.auth0ManagementUserService = auth0ManagementUserService;
	}

	@Override
	@Transactional
	public AppleSignInLifecycleAction applyRelayEmailEvent(final AppleSignInNotification notification,
			final AppleSignInNotificationClaims claims) {
		if (!claims.eventType().isRelayEmailEvent()) {
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
		final boolean forwardingEnabled = claims.eventType() == AppleSignInEventType.EMAIL_ENABLED;
		if (isAlreadyApplied(paciente, claims, forwardingEnabled)) {
			return AppleSignInLifecycleAction.ALREADY_APPLIED;
		}
		final boolean contactEmailProtected = applyRelayMetadata(paciente, claims, forwardingEnabled);
		pacienteRepository.save(paciente);
		try {
			updateAuth0RelayMetadata(notification, claims, forwardingEnabled);
		}
		catch (Auth0ManagementApiException ex) {
			if (log.isWarnEnabled()) {
				log.warn("Auth0 relay metadata update failed for pacienteId={}", notification.getPacienteId());
			}
			return AppleSignInLifecycleAction.AUTH0_UPDATE_FAILED;
		}
		if (!forwardingEnabled && log.isWarnEnabled()) {
			log.warn("Apple relay forwarding disabled; admin review recommended pacienteId={}", paciente.getId());
		}
		if (contactEmailProtected && log.isDebugEnabled()) {
			log.debug("Preserved verified contact email for pacienteId={}", paciente.getId());
		}
		if (log.isInfoEnabled()) {
			log.info("Applied Apple relay metadata pacienteId={} forwardingEnabled={}", paciente.getId(),
					forwardingEnabled);
		}
		return relayActionFor(forwardingEnabled, contactEmailProtected);
	}

	private static boolean isAlreadyApplied(final Paciente paciente, final AppleSignInNotificationClaims claims,
			final boolean forwardingEnabled) {
		if (paciente.getAppleRelayForwardingEnabled() == null) {
			return false;
		}
		if (!paciente.getAppleRelayForwardingEnabled().equals(forwardingEnabled)) {
			return false;
		}
		if (StringUtils.hasText(claims.email()) && StringUtils.hasText(paciente.getAppleRelayEmail())) {
			return claims.email().trim().equalsIgnoreCase(paciente.getAppleRelayEmail());
		}
		return true;
	}

	private static boolean applyRelayMetadata(final Paciente paciente, final AppleSignInNotificationClaims claims,
			final boolean forwardingEnabled) {
		final String relayEmail = StringUtils.hasText(claims.email()) ? claims.email().trim() : null;
		final boolean contactEmailProtected = relayEmail != null && !mayUpdateContactEmail(paciente, relayEmail);
		if (relayEmail != null) {
			paciente.setAppleRelayEmail(relayEmail);
		}
		if (claims.isPrivateEmail() != null) {
			paciente.setAppleRelayPrivateEmail(claims.isPrivateEmail());
		}
		paciente.setAppleRelayForwardingEnabled(forwardingEnabled);
		paciente.setAppleRelayUpdatedAt(Instant.now());
		if (relayEmail != null && !contactEmailProtected) {
			paciente.setEmail(relayEmail);
		}
		return contactEmailProtected;
	}

	private static boolean mayUpdateContactEmail(final Paciente paciente, final String newRelayEmail) {
		final String currentEmail = paciente.getEmail();
		if (!StringUtils.hasText(currentEmail)) {
			return true;
		}
		if (AppleRelayEmailSupport.isApplePrivateRelayEmail(currentEmail)) {
			return true;
		}
		if (StringUtils.hasText(paciente.getAppleRelayEmail())
				&& currentEmail.equalsIgnoreCase(paciente.getAppleRelayEmail())) {
			return true;
		}
		return false;
	}

	private void updateAuth0RelayMetadata(final AppleSignInNotification notification,
			final AppleSignInNotificationClaims claims, final boolean forwardingEnabled) {
		if (!StringUtils.hasText(notification.getAuth0UserId()) || !auth0ManagementUserService.isConfigured()) {
			return;
		}
		final HashMap<String, Object> metadata = new HashMap<>();
		metadata.put("apple_relay_forwarding_enabled", forwardingEnabled);
		metadata.put("apple_relay_updated_at", Instant.now().toString());
		metadata.put("apple_relay_last_event_id", notification.getAppleEventId());
		metadata.put("apple_relay_private_email", claims.isPrivateEmail() != null ? claims.isPrivateEmail() : false);
		if (StringUtils.hasText(claims.email())) {
			metadata.put("apple_relay_email", claims.email().trim());
		}
		auth0ManagementUserService.updateAppMetadata(notification.getAuth0UserId(), metadata);
	}

	private static AppleSignInLifecycleAction relayActionFor(final boolean forwardingEnabled,
			final boolean contactEmailProtected) {
		if (forwardingEnabled) {
			return contactEmailProtected ? AppleSignInLifecycleAction.APPLIED_RELAY_FORWARDING_ENABLED_CONTACT_PROTECTED
					: AppleSignInLifecycleAction.APPLIED_RELAY_FORWARDING_ENABLED;
		}
		return contactEmailProtected ? AppleSignInLifecycleAction.APPLIED_RELAY_FORWARDING_DISABLED_CONTACT_PROTECTED
				: AppleSignInLifecycleAction.APPLIED_RELAY_FORWARDING_DISABLED;
	}

}
