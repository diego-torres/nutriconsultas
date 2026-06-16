package com.nutriconsultas.admin;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.nutriconsultas.contact.ContactInquiryService;
import com.nutriconsultas.controller.AbstractPlatformAdminController;
import com.nutriconsultas.platform.PlatformAdminAuthorization;

@Controller
@RequestMapping("/admin/platform/contact-inquiries")
public class ContactInquiryAdminController extends AbstractPlatformAdminController {

	private final ContactInquiryService contactInquiryService;

	public ContactInquiryAdminController(final ContactInquiryService contactInquiryService,
			final PlatformAdminAuthorization platformAdminAuthorization) {
		super(platformAdminAuthorization);
		this.contactInquiryService = contactInquiryService;
	}

	@GetMapping
	public String list(@AuthenticationPrincipal final OidcUser principal, final Model model) {
		requirePlatformAdmin(principal, "contact-inquiries.list");
		model.addAttribute("inquiries", contactInquiryService.findAllNewestFirst());
		model.addAttribute("activeMenu", "contact-inquiries");
		return "sbadmin/contact-inquiries/listado";
	}

	@PostMapping("/{id}/read")
	public String markAsRead(@AuthenticationPrincipal final OidcUser principal, @PathVariable final Long id) {
		requirePlatformAdmin(principal, "contact-inquiries.mark-read");
		contactInquiryService.markAsRead(id);
		return "redirect:/admin/platform/contact-inquiries";
	}

	@PostMapping("/{id}/delete")
	public String delete(@AuthenticationPrincipal final OidcUser principal, @PathVariable final Long id) {
		requirePlatformAdmin(principal, "contact-inquiries.delete");
		contactInquiryService.deleteById(id);
		return "redirect:/admin/platform/contact-inquiries";
	}

}
