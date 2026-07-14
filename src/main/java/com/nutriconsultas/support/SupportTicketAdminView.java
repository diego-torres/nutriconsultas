package com.nutriconsultas.support;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import com.nutriconsultas.subscription.PlanTier;

/**
 * Admin inbox row: ticket plus resolved creator label and subscription plan.
 */
public record SupportTicketAdminView(@NonNull SupportTicket ticket, @NonNull String userDisplayLabel,
		@Nullable PlanTier planTier, @NonNull String subscriptionLabel) {

	public String title() {
		return ticket.getTitle();
	}

	public Long ticketId() {
		return ticket.getId();
	}

	public SupportTicketStatus status() {
		return ticket.getStatus();
	}

}
