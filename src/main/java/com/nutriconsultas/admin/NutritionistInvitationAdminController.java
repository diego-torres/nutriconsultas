package com.nutriconsultas.admin;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.nutriconsultas.controller.AbstractPlatformAdminController;
import com.nutriconsultas.platform.PlatformAdminAuthorization;
import com.nutriconsultas.subscription.InvitationStatus;
import com.nutriconsultas.subscription.PlanTier;
import com.nutriconsultas.subscription.invitation.CreatedNutritionistInvitation;
import com.nutriconsultas.subscription.invitation.NutritionistInvitationService;
import com.nutriconsultas.subscription.invitation.ActiveNutritionistUserException;
import com.nutriconsultas.subscription.invitation.PendingNutritionistInvitationException;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/platform/invitations")
public class NutritionistInvitationAdminController extends AbstractPlatformAdminController {

	private final NutritionistInvitationService invitationService;

	public NutritionistInvitationAdminController(final PlatformAdminAuthorization platformAdminAuthorization,
			final NutritionistInvitationService invitationService) {
		super(platformAdminAuthorization);
		this.invitationService = invitationService;
	}

	@GetMapping
	public String list(@AuthenticationPrincipal final OidcUser principal, final Model model,
			@RequestParam(required = false) final Long highlight) {
		requirePlatformAdmin(principal, "invitations.list");
		model.addAttribute("highlightInvitationId", highlight);
		model.addAttribute("planTiers", PlanTier.values());
		model.addAttribute("invitationStatuses", InvitationStatus.values());
		model.addAttribute("activeMenu", "invitations");
		return "sbadmin/platform/invitations/list";
	}

	@GetMapping("/new")
	public String createForm(@AuthenticationPrincipal final OidcUser principal, final Model model) {
		requirePlatformAdmin(principal, "invitations.create");
		if (!model.containsAttribute("form")) {
			model.addAttribute("form", new CreateNutritionistInvitationForm());
		}
		model.addAttribute("planTiers", PlanTier.values());
		model.addAttribute("activeMenu", "invitations");
		return "sbadmin/platform/invitations/form";
	}

	@PostMapping
	public String create(@AuthenticationPrincipal final OidcUser principal,
			@Valid @ModelAttribute("form") final CreateNutritionistInvitationForm form,
			final BindingResult bindingResult, final Model model, final RedirectAttributes redirectAttributes) {
		requirePlatformAdmin(principal, "invitations.create");
		if (bindingResult.hasErrors()) {
			model.addAttribute("planTiers", PlanTier.values());
			model.addAttribute("activeMenu", "invitations");
			return "sbadmin/platform/invitations/form";
		}
		try {
			final CreatedNutritionistInvitation created = invitationService.createInvitation(principal, form.getEmail(),
					form.getPlanTier(), form.isPaymentExempt());
			redirectAttributes.addFlashAttribute("inviteUrl", created.inviteUrl());
			redirectAttributes.addFlashAttribute("successMessage", "Invitación creada correctamente.");
			return "redirect:/admin/platform/invitations";
		}
		catch (PendingNutritionistInvitationException ex) {
			model.addAttribute("planTiers", PlanTier.values());
			model.addAttribute("activeMenu", "invitations");
			model.addAttribute("errorMessage",
					"Ya existe una invitación pendiente para este correo. Cancélela en el listado antes de crear otra.");
			model.addAttribute("conflictingInvitationId", ex.getExistingInvitationId());
			return "sbadmin/platform/invitations/form";
		}
		catch (ActiveNutritionistUserException ex) {
			model.addAttribute("planTiers", PlanTier.values());
			model.addAttribute("activeMenu", "invitations");
			model.addAttribute("errorMessage",
					"Este correo ya tiene acceso activo en la plataforma (invitación aceptada). No se puede enviar otra invitación.");
			model.addAttribute("conflictingInvitationId", ex.getRedeemedInvitationId());
			return "sbadmin/platform/invitations/form";
		}
	}

	@PostMapping("/{id}/cancel")
	public String cancel(@AuthenticationPrincipal final OidcUser principal, @PathVariable final Long id,
			final RedirectAttributes redirectAttributes) {
		requirePlatformAdmin(principal, "invitations.cancel");
		invitationService.cancelInvitation(principal, id);
		redirectAttributes.addFlashAttribute("successMessage", "Invitación cancelada correctamente.");
		return "redirect:/admin/platform/invitations";
	}

	@PostMapping("/{id}/revoke-access")
	public String revokeAccess(@AuthenticationPrincipal final OidcUser principal, @PathVariable final Long id,
			@RequestParam(required = false) final String reason, final RedirectAttributes redirectAttributes) {
		requirePlatformAdmin(principal, "invitations.revoke");
		invitationService.revokeNutritionistAccess(principal, id, reason);
		redirectAttributes.addFlashAttribute("successMessage",
				"Acceso revocado. Los datos del nutriólogo se conservan; puede enviar una nueva invitación.");
		return "redirect:/admin/platform/invitations";
	}

	@PostMapping("/{id}/regenerate-link")
	public String regenerateLink(@AuthenticationPrincipal final OidcUser principal, @PathVariable final Long id,
			final RedirectAttributes redirectAttributes) {
		requirePlatformAdmin(principal, "invitations.link");
		final String inviteUrl = invitationService.regenerateInvitationLink(principal, id);
		redirectAttributes.addFlashAttribute("inviteUrl", inviteUrl);
		redirectAttributes.addFlashAttribute("successMessage",
				"Enlace generado. Si compartiste un enlace anterior, ya no es válido.");
		return "redirect:/admin/platform/invitations?highlight=" + id;
	}

}
