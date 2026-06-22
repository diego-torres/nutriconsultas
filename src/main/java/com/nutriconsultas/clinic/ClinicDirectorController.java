package com.nutriconsultas.clinic;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.nutriconsultas.controller.AbstractAuthorizedController;
import com.nutriconsultas.subscription.clinic.ClinicService;
import com.nutriconsultas.subscription.invitation.ActiveNutritionistUserException;
import com.nutriconsultas.subscription.invitation.ClinicInvitationService;
import com.nutriconsultas.subscription.invitation.CreatedClinicInvitation;
import com.nutriconsultas.subscription.invitation.PendingClinicInvitationException;
import com.nutriconsultas.subscription.SubscriptionLimitExceededException;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/clinic")
public class ClinicDirectorController extends AbstractAuthorizedController {

	private final ClinicService clinicService;

	private final ClinicInvitationService clinicInvitationService;

	public ClinicDirectorController(final ClinicService clinicService,
			final ClinicInvitationService clinicInvitationService) {
		this.clinicService = clinicService;
		this.clinicInvitationService = clinicInvitationService;
	}

	@GetMapping
	public String roster(@AuthenticationPrincipal final OidcUser principal, final Model model) {
		model.addAttribute("roster", clinicService.getDirectorRoster(principal.getSubject()));
		model.addAttribute("inviteForm", new CreateClinicInvitationForm());
		model.addAttribute("activeMenu", "clinic");
		return "sbadmin/clinic/members";
	}

	@PostMapping("/invitations")
	public String createInvitation(@AuthenticationPrincipal final OidcUser principal,
			@Valid @ModelAttribute("inviteForm") final CreateClinicInvitationForm form,
			final BindingResult bindingResult, final Model model, final RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("roster", clinicService.getDirectorRoster(principal.getSubject()));
			model.addAttribute("activeMenu", "clinic");
			model.addAttribute("errorMessage", "Correo electrónico no válido.");
			return "sbadmin/clinic/members";
		}
		try {
			final CreatedClinicInvitation created = clinicInvitationService.createInvitation(principal,
					form.getEmail());
			redirectAttributes.addFlashAttribute("inviteUrl", created.inviteUrl());
			redirectAttributes.addFlashAttribute("successMessage", "Invitación enviada correctamente.");
			return "redirect:/admin/clinic";
		}
		catch (PendingClinicInvitationException ex) {
			model.addAttribute("roster", clinicService.getDirectorRoster(principal.getSubject()));
			model.addAttribute("activeMenu", "clinic");
			model.addAttribute("errorMessage",
					"Ya existe una invitación pendiente para este correo. Cancélala en el listado antes de enviar otra.");
			return "sbadmin/clinic/members";
		}
		catch (ActiveNutritionistUserException ex) {
			model.addAttribute("roster", clinicService.getDirectorRoster(principal.getSubject()));
			model.addAttribute("activeMenu", "clinic");
			model.addAttribute("errorMessage",
					"Este correo ya tiene acceso activo en la plataforma. No se puede enviar otra invitación.");
			return "sbadmin/clinic/members";
		}
		catch (SubscriptionLimitExceededException ex) {
			model.addAttribute("roster", clinicService.getDirectorRoster(principal.getSubject()));
			model.addAttribute("activeMenu", "clinic");
			model.addAttribute("errorMessage",
					"No hay plazas disponibles o la suscripción del consultorio no permite invitar nutriólogos.");
			return "sbadmin/clinic/members";
		}
	}

	@PostMapping("/invitations/{id}/cancel")
	public String cancelInvitation(@AuthenticationPrincipal final OidcUser principal, @PathVariable final Long id,
			final RedirectAttributes redirectAttributes) {
		clinicInvitationService.cancelInvitation(principal, id);
		redirectAttributes.addFlashAttribute("successMessage", "Invitación cancelada.");
		return "redirect:/admin/clinic";
	}

	@PostMapping("/invitations/{id}/regenerate-link")
	public String regenerateInvitationLink(@AuthenticationPrincipal final OidcUser principal,
			@PathVariable final Long id, final RedirectAttributes redirectAttributes) {
		final String inviteUrl = clinicInvitationService.regenerateInvitationLink(principal, id);
		redirectAttributes.addFlashAttribute("inviteUrl", inviteUrl);
		redirectAttributes.addFlashAttribute("successMessage", "Enlace de invitación actualizado.");
		return "redirect:/admin/clinic";
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
