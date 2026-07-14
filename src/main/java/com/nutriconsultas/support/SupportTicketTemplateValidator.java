package com.nutriconsultas.support;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.nutriconsultas.validation.template.BaseTemplateValidator;

/**
 * Thymeleaf mocks for the nutritionist Soporte page.
 */
public class SupportTicketTemplateValidator extends BaseTemplateValidator {

	@Override
	public String getTemplatePathPattern() {
		return "sbadmin/soporte/*";
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
		openTicket.setCreatedAt(Instant.parse("2026-07-10T15:30:00Z"));
		openTicket.setUpdatedAt(Instant.parse("2026-07-10T15:30:00Z"));

		final SupportTicket closedTicket = new SupportTicket();
		closedTicket.setId(2L);
		closedTicket.setUserId("auth0|mock-user");
		closedTicket.setTitle("Duda sobre suscripción");
		closedTicket.setDescription("¿Cómo se renueva el plan Profesional?");
		closedTicket.setStatus(SupportTicketStatus.CLOSED);
		closedTicket.setCreatedAt(Instant.parse("2026-07-01T10:00:00Z"));
		closedTicket.setUpdatedAt(Instant.parse("2026-07-02T12:00:00Z"));
		closedTicket.setClosedAt(Instant.parse("2026-07-02T12:00:00Z"));

		variables.put("tickets", List.of(openTicket, closedTicket));
		variables.put("form", new SupportTicketForm());
		variables.put("activeMenu", "soporte");
		return variables;
	}

}
