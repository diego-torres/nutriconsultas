package com.nutriconsultas.support;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.nutriconsultas.controller.AbstractPlatformAdminController;
import com.nutriconsultas.platform.PlatformAdminAuthorization;
import com.nutriconsultas.util.LogRedaction;

import lombok.extern.slf4j.Slf4j;

/**
 * Platform-admin Soporte inbox: list/filter tickets, update notes, close/reopen.
 */
@Controller
@RequestMapping("/admin/platform/soporte")
@Slf4j
public class SupportTicketAdminController extends AbstractPlatformAdminController {

	static final String FILTER_ACTIVOS = "activos";

	static final String FILTER_CERRADOS = "cerrados";

	private final SupportTicketService supportTicketService;

	public SupportTicketAdminController(final PlatformAdminAuthorization platformAdminAuthorization,
			final SupportTicketService supportTicketService) {
		super(platformAdminAuthorization);
		this.supportTicketService = supportTicketService;
	}

	@GetMapping
	public String list(@AuthenticationPrincipal final OidcUser principal, final Model model,
			@RequestParam(name = "estado", defaultValue = FILTER_ACTIVOS) final String estado) {
		requirePlatformAdmin(principal, "soporte.list");
		final String filter = normalizeFilter(estado);
		final SupportTicketStatus status = FILTER_CERRADOS.equals(filter) ? SupportTicketStatus.CLOSED
				: SupportTicketStatus.OPEN;
		model.addAttribute("adminTickets", supportTicketService.findByStatusForAdmin(status));
		model.addAttribute("estado", filter);
		model.addAttribute("activeMenu", "soporte-admin");
		return "sbadmin/platform/soporte/listado";
	}

	@PostMapping("/{id}/notes")
	public String updateNotes(@AuthenticationPrincipal final OidcUser principal, @PathVariable final Long id,
			@RequestParam(required = false) final String adminNotes,
			@RequestParam(name = "estado", defaultValue = FILTER_ACTIVOS) final String estado,
			final RedirectAttributes redirectAttributes) {
		requirePlatformAdmin(principal, "soporte.update-notes");
		final SupportTicket saved = supportTicketService.updateAdminNotes(id, adminNotes);
		if (log.isInfoEnabled()) {
			log.info("Admin updated support ticket notes: {}", LogRedaction.redactSupportTicket(saved.getId()));
		}
		redirectAttributes.addFlashAttribute("successMessage", "Notas actualizadas correctamente.");
		return redirectToList(estado);
	}

	@PostMapping("/{id}/close")
	public String close(@AuthenticationPrincipal final OidcUser principal, @PathVariable final Long id,
			@RequestParam(name = "estado", defaultValue = FILTER_ACTIVOS) final String estado,
			final RedirectAttributes redirectAttributes) {
		requirePlatformAdmin(principal, "soporte.close");
		final SupportTicket saved = supportTicketService.close(id);
		if (log.isInfoEnabled()) {
			log.info("Admin closed support ticket: {}", LogRedaction.redactSupportTicket(saved.getId()));
		}
		redirectAttributes.addFlashAttribute("successMessage", "Ticket cerrado correctamente.");
		return redirectToList(estado);
	}

	@PostMapping("/{id}/reopen")
	public String reopen(@AuthenticationPrincipal final OidcUser principal, @PathVariable final Long id,
			@RequestParam(name = "estado", defaultValue = FILTER_CERRADOS) final String estado,
			final RedirectAttributes redirectAttributes) {
		requirePlatformAdmin(principal, "soporte.reopen");
		final SupportTicket saved = supportTicketService.reopen(id);
		if (log.isInfoEnabled()) {
			log.info("Admin reopened support ticket: {}", LogRedaction.redactSupportTicket(saved.getId()));
		}
		redirectAttributes.addFlashAttribute("successMessage", "Ticket reabierto correctamente.");
		return redirectToList(estado);
	}

	private static String normalizeFilter(final String estado) {
		String result = FILTER_ACTIVOS;
		if (FILTER_CERRADOS.equalsIgnoreCase(estado)) {
			result = FILTER_CERRADOS;
		}
		return result;
	}

	private static String redirectToList(final String estado) {
		return "redirect:/admin/platform/soporte?estado=" + normalizeFilter(estado);
	}

}
