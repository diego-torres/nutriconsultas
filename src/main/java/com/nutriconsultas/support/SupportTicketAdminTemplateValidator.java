package com.nutriconsultas.support;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.nutriconsultas.subscription.PlanTier;
import com.nutriconsultas.validation.template.BaseTemplateValidator;

/**
 * Thymeleaf mocks for the platform-admin Soporte inbox.
 */
public class SupportTicketAdminTemplateValidator extends BaseTemplateValidator {

	@Override
	public String getTemplatePathPattern() {
		return "sbadmin/platform/soporte/*";
	}

	@Override
	public Map<String, Object> createMockModelVariables() {
		final Map<String, Object> variables = super.createMockModelVariables();

		final SupportTicket openTicket = new SupportTicket();
		openTicket.setId(1L);
		openTicket.setUserId("auth0|mock-user");
		openTicket.setTitle("No puedo guardar una dieta");
		openTicket.setDescription("Al intentar guardar la dieta aparece un error inesperado.");
		openTicket.setStatus(SupportTicketStatus.OPEN);
		openTicket.setAdminNotes("Pendiente de revisión");
		openTicket.setCreatedAt(Instant.parse("2026-07-10T15:30:00Z"));
		openTicket.setUpdatedAt(Instant.parse("2026-07-10T15:30:00Z"));

		final SupportTicketAdminView openView = new SupportTicketAdminView(openTicket, "Dra. Prueba",
				PlanTier.PROFESIONAL, PlanTier.PROFESIONAL.getDisplayName());

		variables.put("adminTickets", List.of(openView));
		variables.put("estado", "activos");
		variables.put("activeMenu", "soporte-admin");
		variables.put("platformAdmin", true);
		return variables;
	}

}
