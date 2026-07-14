package com.nutriconsultas.support;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.nutriconsultas.controller.AbstractAuthorizedController;
import com.nutriconsultas.util.LogRedaction;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * Nutritionist Soporte page: list own tickets and create new ones.
 */
@Controller
@RequestMapping("/admin/soporte")
@Slf4j
public class SupportTicketController extends AbstractAuthorizedController {

	private final SupportTicketService supportTicketService;

	public SupportTicketController(final SupportTicketService supportTicketService) {
		this.supportTicketService = supportTicketService;
	}

	@GetMapping
	public String list(@AuthenticationPrincipal final OidcUser principal, final Model model) {
		final String userId = principal.getSubject();
		model.addAttribute("tickets", supportTicketService.findOwnTickets(userId));
		if (!model.containsAttribute("form")) {
			model.addAttribute("form", new SupportTicketForm());
		}
		model.addAttribute("activeMenu", "soporte");
		return "sbadmin/soporte/listado";
	}

	@PostMapping
	public String create(@AuthenticationPrincipal final OidcUser principal,
			@Valid @ModelAttribute("form") final SupportTicketForm form, final BindingResult bindingResult,
			final Model model, final RedirectAttributes redirectAttributes) {
		final String userId = principal.getSubject();
		if (bindingResult.hasErrors()) {
			model.addAttribute("tickets", supportTicketService.findOwnTickets(userId));
			model.addAttribute("activeMenu", "soporte");
			return "sbadmin/soporte/listado";
		}
		final SupportTicket saved = supportTicketService.create(userId, form.getTitle(), form.getDescription());
		if (log.isInfoEnabled()) {
			log.info("Support ticket submitted from Soporte UI: {}", LogRedaction.redactSupportTicket(saved.getId()));
		}
		redirectAttributes.addFlashAttribute("successMessage", "Ticket creado correctamente.");
		return "redirect:/admin/soporte";
	}

}
