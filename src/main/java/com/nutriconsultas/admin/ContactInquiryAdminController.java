package com.nutriconsultas.admin;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.contact.ContactInquiryService;
import com.nutriconsultas.controller.AbstractAuthorizedController;
import com.nutriconsultas.platform.PlatformAdminService;

@Controller
@RequestMapping("/admin/contact-inquiries")
public class ContactInquiryAdminController extends AbstractAuthorizedController {

	private final ContactInquiryService contactInquiryService;

	private final PlatformAdminService platformAdminService;

	public ContactInquiryAdminController(final ContactInquiryService contactInquiryService,
			final PlatformAdminService platformAdminService) {
		this.contactInquiryService = contactInquiryService;
		this.platformAdminService = platformAdminService;
	}

	@GetMapping
	public String list(@AuthenticationPrincipal final OidcUser principal, final Model model) {
		requirePlatformAdmin(principal);
		model.addAttribute("inquiries", contactInquiryService.findAllNewestFirst());
		model.addAttribute("activeMenu", "contact-inquiries");
		return "sbadmin/contact-inquiries/listado";
	}

	@PostMapping("/{id}/read")
	public String markAsRead(@AuthenticationPrincipal final OidcUser principal, @PathVariable final Long id) {
		requirePlatformAdmin(principal);
		contactInquiryService.markAsRead(id);
		return "redirect:/admin/contact-inquiries";
	}

	@PostMapping("/{id}/delete")
	public String delete(@AuthenticationPrincipal final OidcUser principal, @PathVariable final Long id) {
		requirePlatformAdmin(principal);
		contactInquiryService.deleteById(id);
		return "redirect:/admin/contact-inquiries";
	}

	private void requirePlatformAdmin(final OidcUser principal) {
		if (!platformAdminService.isPlatformAdmin(principal)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
	}

}
