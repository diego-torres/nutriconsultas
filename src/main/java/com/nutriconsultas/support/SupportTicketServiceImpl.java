package com.nutriconsultas.support;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.subscription.PlanTier;
import com.nutriconsultas.subscription.SubscriptionEntitlementService;
import com.nutriconsultas.subscription.clinic.ClinicMemberLabelResolver;
import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SupportTicketServiceImpl implements SupportTicketService {

	private static final String NO_SUBSCRIPTION_LABEL = "Sin suscripción";

	private final SupportTicketRepository supportTicketRepository;

	private final ClinicMemberLabelResolver clinicMemberLabelResolver;

	private final SubscriptionEntitlementService subscriptionEntitlementService;

	public SupportTicketServiceImpl(final SupportTicketRepository supportTicketRepository,
			final ClinicMemberLabelResolver clinicMemberLabelResolver,
			final SubscriptionEntitlementService subscriptionEntitlementService) {
		this.supportTicketRepository = supportTicketRepository;
		this.clinicMemberLabelResolver = clinicMemberLabelResolver;
		this.subscriptionEntitlementService = subscriptionEntitlementService;
	}

	@Override
	@Transactional
	public SupportTicket create(@NonNull final String userId, @NonNull final String title,
			@NonNull final String description) {
		requireText(userId, "El usuario es obligatorio");
		final String trimmedTitle = requireText(title, "El título es obligatorio");
		final String trimmedDescription = requireText(description, "La descripción es obligatoria");
		if (trimmedTitle.length() > 200) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El título es demasiado largo");
		}
		if (trimmedDescription.length() > 4000) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La descripción es demasiado larga");
		}

		final SupportTicket ticket = new SupportTicket();
		ticket.setUserId(userId);
		ticket.setTitle(trimmedTitle);
		ticket.setDescription(trimmedDescription);
		ticket.setStatus(SupportTicketStatus.OPEN);

		final SupportTicket saved = supportTicketRepository.save(ticket);
		if (log.isInfoEnabled()) {
			log.info("Support ticket created: {} for {}", LogRedaction.redactSupportTicket(saved.getId()),
					LogRedaction.redactUserId(userId));
		}
		return saved;
	}

	@Override
	@Transactional(readOnly = true)
	public List<SupportTicket> findOwnTickets(@NonNull final String userId) {
		requireText(userId, "El usuario es obligatorio");
		return supportTicketRepository.findByUserIdOrderByCreatedAtDesc(userId);
	}

	@Override
	@Transactional(readOnly = true)
	public SupportTicket findOwnTicket(@NonNull final String userId, @NonNull final Long id) {
		requireText(userId, "El usuario es obligatorio");
		return supportTicketRepository.findByIdAndUserId(id, userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

	@Override
	@Transactional(readOnly = true)
	public List<SupportTicketAdminView> findAllForAdmin() {
		return supportTicketRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toAdminView).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<SupportTicketAdminView> findByStatusForAdmin(@NonNull final SupportTicketStatus status) {
		if (status == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado es obligatorio");
		}
		return supportTicketRepository.findByStatusOrderByCreatedAtDesc(status)
			.stream()
			.map(this::toAdminView)
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public SupportTicketAdminView findByIdForAdmin(@NonNull final Long id) {
		return toAdminView(requireTicket(id));
	}

	@Override
	@Transactional
	public SupportTicket updateAdminNotes(@NonNull final Long id, final String adminNotes) {
		final SupportTicket ticket = requireTicket(id);
		final String notes = adminNotes == null ? null : adminNotes.trim();
		if (notes != null && notes.length() > 2000) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las notas son demasiado largas");
		}
		ticket.setAdminNotes(StringUtils.hasText(notes) ? notes : null);
		final SupportTicket saved = supportTicketRepository.save(ticket);
		if (log.isInfoEnabled()) {
			log.info("Support ticket admin notes updated: {}", LogRedaction.redactSupportTicket(saved.getId()));
		}
		return saved;
	}

	@Override
	@Transactional
	public SupportTicket close(@NonNull final Long id) {
		final SupportTicket ticket = requireTicket(id);
		if (ticket.getStatus() == SupportTicketStatus.CLOSED) {
			return ticket;
		}
		ticket.setStatus(SupportTicketStatus.CLOSED);
		ticket.setClosedAt(Instant.now());
		final SupportTicket saved = supportTicketRepository.save(ticket);
		if (log.isInfoEnabled()) {
			log.info("Support ticket closed: {}", LogRedaction.redactSupportTicket(saved.getId()));
		}
		return saved;
	}

	@Override
	@Transactional
	public SupportTicket reopen(@NonNull final Long id) {
		final SupportTicket ticket = requireTicket(id);
		if (ticket.getStatus() == SupportTicketStatus.OPEN) {
			return ticket;
		}
		ticket.setStatus(SupportTicketStatus.OPEN);
		ticket.setClosedAt(null);
		final SupportTicket saved = supportTicketRepository.save(ticket);
		if (log.isInfoEnabled()) {
			log.info("Support ticket reopened: {}", LogRedaction.redactSupportTicket(saved.getId()));
		}
		return saved;
	}

	private SupportTicket requireTicket(final Long id) {
		return supportTicketRepository.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

	private SupportTicketAdminView toAdminView(final SupportTicket ticket) {
		final String userId = ticket.getUserId();
		final String userLabel = clinicMemberLabelResolver.resolveLabel(userId);
		final PlanTier planTier = subscriptionEntitlementService.getEffectivePlanTier(userId).orElse(null);
		final String subscriptionLabel = planTier != null ? planTier.getDisplayName() : NO_SUBSCRIPTION_LABEL;
		return new SupportTicketAdminView(ticket, userLabel, planTier, subscriptionLabel);
	}

	private static String requireText(final String value, final String message) {
		if (!StringUtils.hasText(value)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
		}
		return value.trim();
	}

}
