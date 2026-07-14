package com.nutriconsultas.support;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nutriconsultas.subscription.PlanTier;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SupportTicketWebMvcTest {

	private static final String NUTRITIONIST_USER_ID = "auth0|nutritionist-support-test";

	private static final String PLATFORM_ADMIN_USER_ID = "auth0|platform-admin-test";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SupportTicketService supportTicketService;

	@Test
	void soporte_whenUnauthenticated_redirectsToLogin() throws Exception {
		mockMvc.perform(get("/admin/soporte"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrlPattern("**/oauth2/authorization/**"));
	}

	@Test
	void soporte_whenNutritionist_returnsOwnTicketsView() throws Exception {
		final SupportTicket ticket = sampleTicket(1L, NUTRITIONIST_USER_ID, SupportTicketStatus.OPEN);
		when(supportTicketService.findOwnTickets(NUTRITIONIST_USER_ID)).thenReturn(List.of(ticket));

		mockMvc
			.perform(get("/admin/soporte").with(oidcLogin().idToken(token -> token.subject(NUTRITIONIST_USER_ID)
				.claim("name", "Nutrióloga Test")
				.claim("email", "nutri@example.com"))))
			.andExpect(status().isOk())
			.andExpect(view().name("sbadmin/soporte/listado"))
			.andExpect(model().attributeExists("tickets"))
			.andExpect(model().attributeExists("form"))
			.andExpect(model().attribute("activeMenu", "soporte"))
			.andExpect(model().attributeExists("appVersion"));
	}

	@Test
	void soporte_whenPlatformAdmin_redirectsToAdminInbox() throws Exception {
		mockMvc
			.perform(get("/admin/soporte").with(oidcLogin().idToken(token -> token.subject(PLATFORM_ADMIN_USER_ID)
				.claim("name", "Admin Test")
				.claim("email", "admin@example.com"))))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/admin/platform/soporte"));
	}

	@Test
	void createTicket_whenNutritionist_redirectsWithFlash() throws Exception {
		final SupportTicket saved = sampleTicket(9L, NUTRITIONIST_USER_ID, SupportTicketStatus.OPEN);
		when(supportTicketService.create(eq(NUTRITIONIST_USER_ID), eq("Falla al guardar"), eq("Detalle del error")))
			.thenReturn(saved);

		mockMvc
			.perform(post("/admin/soporte")
				.with(oidcLogin().idToken(token -> token.subject(NUTRITIONIST_USER_ID)
					.claim("name", "Nutrióloga Test")
					.claim("email", "nutri@example.com")))
				.with(csrf())
				.param("title", "Falla al guardar")
				.param("description", "Detalle del error"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/admin/soporte"))
			.andExpect(flash().attribute("successMessage", "Ticket creado correctamente."));

		verify(supportTicketService).create(NUTRITIONIST_USER_ID, "Falla al guardar", "Detalle del error");
	}

	@Test
	void adminSoporte_whenNutritionist_forbidden() throws Exception {
		mockMvc.perform(
				get("/admin/platform/soporte").with(oidcLogin().idToken(token -> token.subject(NUTRITIONIST_USER_ID)
					.claim("name", "Nutrióloga Test")
					.claim("email", "nutri@example.com"))))
			.andExpect(status().isForbidden());
	}

	@Test
	void adminSoporte_whenPlatformAdmin_returnsInbox() throws Exception {
		final SupportTicket ticket = sampleTicket(3L, NUTRITIONIST_USER_ID, SupportTicketStatus.OPEN);
		final SupportTicketAdminView view = new SupportTicketAdminView(ticket, "Nutrióloga Test", PlanTier.PLUS,
				"Plus");
		when(supportTicketService.findByStatusForAdmin(SupportTicketStatus.OPEN)).thenReturn(List.of(view));

		mockMvc
			.perform(get("/admin/platform/soporte")
				.with(oidcLogin().idToken(token -> token.subject(PLATFORM_ADMIN_USER_ID)
					.claim("name", "Admin Test")
					.claim("email", "admin@example.com"))))
			.andExpect(status().isOk())
			.andExpect(view().name("sbadmin/platform/soporte/listado"))
			.andExpect(model().attributeExists("adminTickets"))
			.andExpect(model().attribute("estado", "activos"))
			.andExpect(model().attribute("activeMenu", "soporte-admin"))
			.andExpect(model().attributeExists("appVersion"));
	}

	@Test
	void adminClose_whenNutritionist_forbidden() throws Exception {
		mockMvc.perform(post("/admin/platform/soporte/1/close")
			.with(oidcLogin().idToken(token -> token.subject(NUTRITIONIST_USER_ID)
				.claim("name", "Nutrióloga Test")
				.claim("email", "nutri@example.com")))
			.with(csrf())).andExpect(status().isForbidden());
	}

	@Test
	void adminClose_whenPlatformAdmin_redirects() throws Exception {
		when(supportTicketService.close(1L))
			.thenReturn(sampleTicket(1L, NUTRITIONIST_USER_ID, SupportTicketStatus.CLOSED));

		mockMvc
			.perform(post("/admin/platform/soporte/1/close").param("estado", "activos")
				.with(oidcLogin().idToken(token -> token.subject(PLATFORM_ADMIN_USER_ID)
					.claim("name", "Admin Test")
					.claim("email", "admin@example.com")))
				.with(csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/admin/platform/soporte?estado=activos"))
			.andExpect(flash().attribute("successMessage", "Ticket cerrado correctamente."));

		verify(supportTicketService).close(1L);
	}

	@Test
	void adminUpdateNotes_whenPlatformAdmin_redirects() throws Exception {
		when(supportTicketService.updateAdminNotes(eq(2L), anyString()))
			.thenReturn(sampleTicket(2L, NUTRITIONIST_USER_ID, SupportTicketStatus.OPEN));

		mockMvc
			.perform(post("/admin/platform/soporte/2/notes").param("estado", "activos")
				.param("adminNotes", "En revisión")
				.with(oidcLogin().idToken(token -> token.subject(PLATFORM_ADMIN_USER_ID)
					.claim("name", "Admin Test")
					.claim("email", "admin@example.com")))
				.with(csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/admin/platform/soporte?estado=activos"))
			.andExpect(flash().attribute("successMessage", "Notas actualizadas correctamente."));

		verify(supportTicketService).updateAdminNotes(2L, "En revisión");
	}

	private static SupportTicket sampleTicket(final Long id, final String userId, final SupportTicketStatus status) {
		final SupportTicket ticket = new SupportTicket();
		ticket.setId(id);
		ticket.setUserId(userId);
		ticket.setTitle("Asunto de prueba");
		ticket.setDescription("Descripción de prueba");
		ticket.setStatus(status);
		ticket.setCreatedAt(Instant.parse("2026-07-10T12:00:00Z"));
		ticket.setUpdatedAt(Instant.parse("2026-07-10T12:00:00Z"));
		return ticket;
	}

}
