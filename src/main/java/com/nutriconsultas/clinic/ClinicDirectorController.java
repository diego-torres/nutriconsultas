package com.nutriconsultas.clinic;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.nutriconsultas.controller.AbstractAuthorizedController;
import com.nutriconsultas.subscription.clinic.ClinicService;

@Controller
@RequestMapping("/admin/clinic")
public class ClinicDirectorController extends AbstractAuthorizedController {

	private final ClinicService clinicService;

	public ClinicDirectorController(final ClinicService clinicService) {
		this.clinicService = clinicService;
	}

	@GetMapping
	public String roster(@AuthenticationPrincipal final OidcUser principal, final Model model) {
		model.addAttribute("roster", clinicService.getDirectorRoster(principal.getSubject()));
		model.addAttribute("activeMenu", "clinic");
		return "sbadmin/clinic/members";
	}

	@PostMapping("/members/{memberId}/suspend")
	public String suspendMember(@AuthenticationPrincipal final OidcUser principal, @PathVariable final Long memberId,
			final RedirectAttributes redirectAttributes) {
		clinicService.suspendMember(principal.getSubject(), memberId);
		redirectAttributes.addFlashAttribute("successMessage", "Acceso del nutriólogo suspendido.");
		return "redirect:/admin/clinic";
	}

	@PostMapping("/members/{memberId}/reactivate")
	public String reactivateMember(@AuthenticationPrincipal final OidcUser principal, @PathVariable final Long memberId,
			final RedirectAttributes redirectAttributes) {
		clinicService.reactivateMember(principal.getSubject(), memberId);
		redirectAttributes.addFlashAttribute("successMessage", "Acceso del nutriólogo reactivado.");
		return "redirect:/admin/clinic";
	}

}
