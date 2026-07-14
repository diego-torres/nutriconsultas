package com.nutriconsultas.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.nutriconsultas.platform.PlatformAdminService;

@ExtendWith(MockitoExtension.class)
class SupportTicketControllerTest {

	private static final String USER_ID = "auth0|nutritionist-1";

	@InjectMocks
	private SupportTicketController controller;

	@Mock
	private SupportTicketService supportTicketService;

	@Mock
	private PlatformAdminService platformAdminService;

	@Test
	void list_addsOwnTicketsAndEmptyForm() {
		final DefaultOidcUser principal = principal();
		final SupportTicket ticket = sampleTicket(1L, SupportTicketStatus.OPEN);
		when(platformAdminService.isPlatformAdmin(principal)).thenReturn(false);
		when(supportTicketService.findOwnTickets(USER_ID)).thenReturn(List.of(ticket));
		final ExtendedModelMap model = new ExtendedModelMap();

		final String view = controller.list(principal, model);

		assertThat(view).isEqualTo("sbadmin/soporte/listado");
		assertThat(model.getAttribute("tickets")).isEqualTo(List.of(ticket));
		assertThat(model.getAttribute("form")).isInstanceOf(SupportTicketForm.class);
		assertThat(model.getAttribute("activeMenu")).isEqualTo("soporte");
		verify(supportTicketService).findOwnTickets(USER_ID);
	}

	@Test
	void list_whenPlatformAdmin_redirectsToAdminInbox() {
		final DefaultOidcUser principal = principal();
		when(platformAdminService.isPlatformAdmin(principal)).thenReturn(true);

		final String view = controller.list(principal, new ExtendedModelMap());

		assertThat(view).isEqualTo("redirect:/admin/platform/soporte");
	}

	@Test
	void create_whenValid_redirectsWithSuccessFlash() {
		final SupportTicketForm form = new SupportTicketForm();
		form.setTitle("Problema con dietas");
		form.setDescription("No puedo editar ingredientes.");
		final BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
		final SupportTicket saved = sampleTicket(9L, SupportTicketStatus.OPEN);
		when(supportTicketService.create(eq(USER_ID), eq("Problema con dietas"), eq("No puedo editar ingredientes.")))
			.thenReturn(saved);
		final RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

		final String view = controller.create(principal(), form, bindingResult, new ExtendedModelMap(),
				redirectAttributes);

		assertThat(view).isEqualTo("redirect:/admin/soporte");
		assertThat(redirectAttributes.getFlashAttributes().get("successMessage"))
			.isEqualTo("Ticket creado correctamente.");
		verify(supportTicketService).create(USER_ID, "Problema con dietas", "No puedo editar ingredientes.");
	}

	@Test
	void create_whenValidationErrors_returnsListViewWithTickets() {
		final SupportTicketForm form = new SupportTicketForm();
		final BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
		bindingResult.rejectValue("title", "NotBlank", "El título es obligatorio");
		final SupportTicket existing = sampleTicket(3L, SupportTicketStatus.CLOSED);
		when(supportTicketService.findOwnTickets(USER_ID)).thenReturn(List.of(existing));
		final ExtendedModelMap model = new ExtendedModelMap();

		final String view = controller.create(principal(), form, bindingResult, model,
				new RedirectAttributesModelMap());

		assertThat(view).isEqualTo("sbadmin/soporte/listado");
		assertThat(model.getAttribute("tickets")).isEqualTo(List.of(existing));
		assertThat(model.getAttribute("activeMenu")).isEqualTo("soporte");
	}

	private static DefaultOidcUser principal() {
		final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(USER_ID).build();
		final OidcIdToken idToken = new OidcIdToken(jwt.getTokenValue(), jwt.getIssuedAt(), jwt.getExpiresAt(),
				Map.of("sub", USER_ID));
		return new DefaultOidcUser(List.of(), idToken);
	}

	private static SupportTicket sampleTicket(final Long id, final SupportTicketStatus status) {
		final SupportTicket ticket = new SupportTicket();
		ticket.setId(id);
		ticket.setUserId(USER_ID);
		ticket.setTitle("Título de prueba");
		ticket.setDescription("Descripción de prueba");
		ticket.setStatus(status);
		ticket.setCreatedAt(Instant.parse("2026-07-10T12:00:00Z"));
		ticket.setUpdatedAt(Instant.parse("2026-07-10T12:00:00Z"));
		return ticket;
	}

}
