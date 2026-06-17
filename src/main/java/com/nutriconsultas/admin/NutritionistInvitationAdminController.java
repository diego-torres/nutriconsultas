package com.nutriconsultas.admin;

import java.util.List;

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

import com.nutriconsultas.controller.AbstractPlatformAdminController;
import com.nutriconsultas.platform.PlatformAdminAuthorization;
import com.nutriconsultas.subscription.NutritionistInvitation;
import com.nutriconsultas.subscription.NutritionistInvitationRepository;
import com.nutriconsultas.subscription.PlanTier;
import com.nutriconsultas.subscription.invitation.CreatedNutritionistInvitation;
import com.nutriconsultas.subscription.invitation.NutritionistInvitationService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/platform/invitations")
public class NutritionistInvitationAdminController extends AbstractPlatformAdminController {

	private final NutritionistInvitationService invitationService;

	private final NutritionistInvitationRepository invitationRepository;

	public NutritionistInvitationAdminController(final PlatformAdminAuthorization platformAdminAuthorization,
			final NutritionistInvitationService invitationService,
			final NutritionistInvitationRepository invitationRepository) {
		super(platformAdminAuthorization);
		this.invitationService = invitationService;
		this.invitationRepository = invitationRepository;
	}

	@GetMapping
	public String list(@AuthenticationPrincipal final OidcUser principal, final Model model) {
		requirePlatformAdmin(principal, "invitations.list");
		final List<NutritionistInvitation> invitations = invitationRepository.findAllByOrderByCreatedAtDesc();
		model.addAttribute("invitations", invitations);
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
		final CreatedNutritionistInvitation created = invitationService.createInvitation(principal, form.getEmail(),
				form.getPlanTier(), form.isPaymentExempt());
		redirectAttributes.addFlashAttribute("inviteUrl", created.inviteUrl());
		redirectAttributes.addFlashAttribute("successMessage", "Invitación creada correctamente.");
		return "redirect:/admin/platform/invitations";
	}

}
