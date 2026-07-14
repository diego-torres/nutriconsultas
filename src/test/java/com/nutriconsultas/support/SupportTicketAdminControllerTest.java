package com.nutriconsultas.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.nutriconsultas.platform.PlatformAdminAuthorization;
import com.nutriconsultas.subscription.PlanTier;

@ExtendWith(MockitoExtension.class)
class SupportTicketAdminControllerTest {

	@InjectMocks
	private SupportTicketAdminController controller;

	@Mock
	private SupportTicketService supportTicketService;

	@Mock
	private PlatformAdminAuthorization platformAdminAuthorization;

	@Mock
	private OidcUser principal;

	@Test
	void list_whenNotPlatformAdmin_throwsForbidden() {
		doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(platformAdminAuthorization)
			.requirePlatformAdmin(principal, "soporte.list");

		assertThatThrownBy(() -> controller.list(principal, new ExtendedModelMap(), "activos"))
			.isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
			.isEqualTo(403);
	}

	@Test
	void list_defaultsToOpenTickets() {
		final SupportTicketAdminView view = sampleView(SupportTicketStatus.OPEN);
		when(supportTicketService.findByStatusForAdmin(SupportTicketStatus.OPEN)).thenReturn(List.of(view));
		final ExtendedModelMap model = new ExtendedModelMap();

		final String result = controller.list(principal, model, "activos");

		assertThat(result).isEqualTo("sbadmin/platform/soporte/listado");
		assertThat(model.getAttribute("adminTickets")).isEqualTo(List.of(view));
		assertThat(model.getAttribute("estado")).isEqualTo("activos");
		assertThat(model.getAttribute("activeMenu")).isEqualTo("soporte-admin");
		verify(platformAdminAuthorization).requirePlatformAdmin(principal, "soporte.list");
	}

	@Test
	void list_filtersClosedTickets() {
		when(supportTicketService.findByStatusForAdmin(SupportTicketStatus.CLOSED)).thenReturn(List.of());
		final ExtendedModelMap model = new ExtendedModelMap();

		final String result = controller.list(principal, model, "cerrados");

		assertThat(result).isEqualTo("sbadmin/platform/soporte/listado");
		assertThat(model.getAttribute("estado")).isEqualTo("cerrados");
		verify(supportTicketService).findByStatusForAdmin(SupportTicketStatus.CLOSED);
	}

	@Test
	void updateNotes_whenPlatformAdmin_redirects() {
		when(supportTicketService.updateAdminNotes(1L, "Revisado")).thenReturn(sampleTicket(SupportTicketStatus.OPEN));
		final RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

		final String result = controller.updateNotes(principal, 1L, "Revisado", "activos", redirectAttributes);

		assertThat(result).isEqualTo("redirect:/admin/platform/soporte?estado=activos");
		assertThat(redirectAttributes.getFlashAttributes().get("successMessage"))
			.isEqualTo("Notas actualizadas correctamente.");
		verify(platformAdminAuthorization).requirePlatformAdmin(principal, "soporte.update-notes");
	}

	@Test
	void close_whenNotPlatformAdmin_throwsForbidden() {
		doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(platformAdminAuthorization)
			.requirePlatformAdmin(principal, "soporte.close");

		assertThatThrownBy(() -> controller.close(principal, 1L, "activos", new RedirectAttributesModelMap()))
			.isInstanceOf(ResponseStatusException.class)
			.extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
			.isEqualTo(403);
	}

	@Test
	void close_whenPlatformAdmin_redirects() {
		when(supportTicketService.close(1L)).thenReturn(sampleTicket(SupportTicketStatus.CLOSED));
		final RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

		final String result = controller.close(principal, 1L, "activos", redirectAttributes);

		assertThat(result).isEqualTo("redirect:/admin/platform/soporte?estado=activos");
		assertThat(redirectAttributes.getFlashAttributes().get("successMessage"))
			.isEqualTo("Ticket cerrado correctamente.");
		verify(supportTicketService).close(1L);
	}

	@Test
	void reopen_whenPlatformAdmin_redirects() {
		when(supportTicketService.reopen(2L)).thenReturn(sampleTicket(SupportTicketStatus.OPEN));
		final RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

		final String result = controller.reopen(principal, 2L, "cerrados", redirectAttributes);

		assertThat(result).isEqualTo("redirect:/admin/platform/soporte?estado=cerrados");
		assertThat(redirectAttributes.getFlashAttributes().get("successMessage"))
			.isEqualTo("Ticket reabierto correctamente.");
		verify(supportTicketService).reopen(2L);
	}

	private static SupportTicketAdminView sampleView(final SupportTicketStatus status) {
		return new SupportTicketAdminView(sampleTicket(status), "Nutrióloga Demo", PlanTier.PLUS,
				PlanTier.PLUS.getDisplayName());
	}

	private static SupportTicket sampleTicket(final SupportTicketStatus status) {
		final SupportTicket ticket = new SupportTicket();
		ticket.setId(1L);
		ticket.setUserId("auth0|user");
		ticket.setTitle("Ayuda");
		ticket.setDescription("Detalle");
		ticket.setStatus(status);
		ticket.setCreatedAt(Instant.parse("2026-07-10T12:00:00Z"));
		ticket.setUpdatedAt(Instant.parse("2026-07-10T12:00:00Z"));
		return ticket;
	}

}
