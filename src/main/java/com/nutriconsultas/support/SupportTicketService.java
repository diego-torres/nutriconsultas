package com.nutriconsultas.support;

import java.util.List;

import org.springframework.lang.NonNull;

/**
 * In-app Soporte tickets: nutritionist own-ticket operations and platform-admin triage.
 */
public interface SupportTicketService {

	SupportTicket create(@NonNull String userId, @NonNull String title, @NonNull String description);

	List<SupportTicket> findOwnTickets(@NonNull String userId);

	SupportTicket findOwnTicket(@NonNull String userId, @NonNull Long id);

	List<SupportTicketAdminView> findAllForAdmin();

	List<SupportTicketAdminView> findByStatusForAdmin(@NonNull SupportTicketStatus status);

	SupportTicketAdminView findByIdForAdmin(@NonNull Long id);

	SupportTicket updateAdminNotes(@NonNull Long id, String adminNotes);

	SupportTicket close(@NonNull Long id);

	SupportTicket reopen(@NonNull Long id);

}
