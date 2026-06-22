package com.nutriconsultas.paciente.invitation;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.mobile.dto.CreatePatientInvitationRequest;
import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteRepository;
import com.nutriconsultas.paciente.PacienteStatus;
import com.nutriconsultas.paciente.PatientInvitation;
import com.nutriconsultas.paciente.PatientInvitationRepository;
import com.nutriconsultas.paciente.PatientInvitationStatus;
import com.nutriconsultas.subscription.SubscriptionEntitlementService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PatientInvitationCreateServiceImpl implements PatientInvitationCreateService {

	private final SubscriptionEntitlementService subscriptionEntitlementService;

	private final PacienteRepository pacienteRepository;

	private final PatientInvitationRepository patientInvitationRepository;

	private final PatientInvitationTokenService patientInvitationTokenService;

	private final PatientInvitationProperties invitationProperties;

	private final PatientInvitationEmailSender patientInvitationEmailSender;

	public PatientInvitationCreateServiceImpl(final SubscriptionEntitlementService subscriptionEntitlementService,
			final PacienteRepository pacienteRepository, final PatientInvitationRepository patientInvitationRepository,
			final PatientInvitationTokenService patientInvitationTokenService,
			final PatientInvitationProperties invitationProperties,
			final PatientInvitationEmailSender patientInvitationEmailSender) {
		this.subscriptionEntitlementService = subscriptionEntitlementService;
		this.pacienteRepository = pacienteRepository;
		this.patientInvitationRepository = patientInvitationRepository;
		this.patientInvitationTokenService = patientInvitationTokenService;
		this.invitationProperties = invitationProperties;
		this.patientInvitationEmailSender = patientInvitationEmailSender;
	}

	@Override
	@Transactional
	public CreatedPatientInvitationResult createInvitation(final String nutritionistUserId,
			final CreatePatientInvitationRequest request) {
		if (!StringUtils.hasText(nutritionistUserId)) {
			throw new IllegalArgumentException("nutritionistUserId is required");
		}
		subscriptionEntitlementService.assertCanCreatePatient(nutritionistUserId);

		final String normalizedEmail = normalizeEmail(request.email());
		final String assignedId = resolveAssignedId(request.assignedId());
		final PatientInvitationTokenBundle tokenBundle = patientInvitationTokenService.generate();
		final Instant expiresAt = Instant.now().plus(invitationProperties.getExpiryDays(), ChronoUnit.DAYS);

		final Paciente paciente = buildPaciente(nutritionistUserId, request, normalizedEmail, assignedId);
		final Paciente savedPaciente = pacienteRepository.save(paciente);

		final PatientInvitation invitation = new PatientInvitation();
		invitation.setTokenHash(tokenBundle.tokenHash());
		invitation.setPaciente(savedPaciente);
		invitation.setNutritionistUserId(nutritionistUserId);
		invitation.setStatus(PatientInvitationStatus.PENDING);
		invitation.setExpiresAt(expiresAt);
		invitation.setMaxUses(1);
		final PatientInvitation savedInvitation = patientInvitationRepository.save(invitation);

		final String inviteUrl = invitationProperties.buildInviteUrl(tokenBundle.urlToken());
		patientInvitationEmailSender.sendPatientInvitation(normalizedEmail, tokenBundle.humanCode(), inviteUrl);

		final Optional<String> offlineJws = patientInvitationTokenService.createOfflineJws(savedPaciente.getId(),
				tokenBundle.urlToken(), expiresAt);

		if (log.isInfoEnabled()) {
			log.info("Created patient invitation: invitationId={}, pacienteId={}", savedInvitation.getId(),
					savedPaciente.getId());
		}

		return new CreatedPatientInvitationResult(savedInvitation.getId(), savedPaciente.getId(), inviteUrl,
				tokenBundle.humanCode(), expiresAt, offlineJws.orElse(null));
	}

	private Paciente buildPaciente(final String nutritionistUserId, final CreatePatientInvitationRequest request,
			final String normalizedEmail, final String assignedId) {
		final Paciente paciente = new Paciente();
		paciente.setUserId(nutritionistUserId);
		paciente.setName(request.name().trim());
		paciente.setEmail(normalizedEmail);
		paciente.setEmailHint(normalizedEmail);
		paciente.setStatus(PacienteStatus.INVITED);
		paciente.setAssignedId(assignedId);
		paciente.setDob(toDate(request.dob()));
		paciente.setGender(request.gender().trim().toUpperCase(Locale.ROOT));
		if (StringUtils.hasText(request.phone())) {
			paciente.setPhone(request.phone().trim());
		}
		if (StringUtils.hasText(request.displayName())) {
			paciente.setDisplayName(request.displayName().trim());
		}
		else {
			paciente.setDisplayName(request.name().trim());
		}
		return paciente;
	}

	private String resolveAssignedId(final String requestedAssignedId) {
		if (StringUtils.hasText(requestedAssignedId)) {
			final String assignedId = requestedAssignedId.trim();
			if (pacienteRepository.existsByAssignedId(assignedId)) {
				throw new ResponseStatusException(HttpStatus.CONFLICT, "assignedId already in use");
			}
			return assignedId;
		}
		String candidate = PatientAssignedIdGenerator.generate();
		while (pacienteRepository.existsByAssignedId(candidate)) {
			candidate = PatientAssignedIdGenerator.generate();
		}
		return candidate;
	}

	private static String normalizeEmail(final String email) {
		if (!StringUtils.hasText(email)) {
			throw new IllegalArgumentException("email is required");
		}
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private static Date toDate(final LocalDate dob) {
		return Date.from(dob.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

}
