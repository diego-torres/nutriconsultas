package com.nutriconsultas.admin;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

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
import com.nutriconsultas.subscription.Clinic;
import com.nutriconsultas.subscription.ClinicRepository;
import com.nutriconsultas.subscription.NutritionistInvitation;
import com.nutriconsultas.subscription.NutritionistInvitationRepository;
import com.nutriconsultas.subscription.PlanTier;
import com.nutriconsultas.subscription.PlanTierChangeResult;
import com.nutriconsultas.subscription.Subscription;
import com.nutriconsultas.subscription.SubscriptionRepository;
import com.nutriconsultas.subscription.SubscriptionStatus;
import com.nutriconsultas.subscription.NutritionistRoleService;
import com.nutriconsultas.subscription.invitation.NutritionistInvitationAccessRules;
import com.nutriconsultas.subscription.invitation.NutritionistInvitationService;
import com.nutriconsultas.subscription.lifecycle.AdminSubscriptionOverride;
import com.nutriconsultas.subscription.lifecycle.SubscriptionLifecycleService;

import jakarta.validation.Valid;

import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/admin/platform/subscriptions")
public class SubscriptionAdminController extends AbstractPlatformAdminController {

	private static final DateTimeFormatter PERIOD_END_INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
		.withZone(ZoneId.of("America/Mexico_City"));

	private final SubscriptionRepository subscriptionRepository;

	private final ClinicRepository clinicRepository;

	private final SubscriptionLifecycleService lifecycleService;

	private final NutritionistInvitationRepository invitationRepository;

	private final NutritionistInvitationService invitationService;

	private final NutritionistRoleService nutritionistRoleService;

	private final SubscriptionOwnerResolver ownerResolver;

	public SubscriptionAdminController(final PlatformAdminAuthorization platformAdminAuthorization,
			final SubscriptionRepository subscriptionRepository, final ClinicRepository clinicRepository,
			final SubscriptionLifecycleService lifecycleService,
			final NutritionistInvitationRepository invitationRepository,
			final NutritionistInvitationService invitationService,
			final NutritionistRoleService nutritionistRoleService, final SubscriptionOwnerResolver ownerResolver) {
		super(platformAdminAuthorization);
		this.subscriptionRepository = subscriptionRepository;
		this.clinicRepository = clinicRepository;
		this.lifecycleService = lifecycleService;
		this.invitationRepository = invitationRepository;
		this.invitationService = invitationService;
		this.nutritionistRoleService = nutritionistRoleService;
		this.ownerResolver = ownerResolver;
	}

	@GetMapping
	public String list(@AuthenticationPrincipal final OidcUser principal, final Model model) {
		requirePlatformAdmin(principal, "subscriptions.list");
		model.addAttribute("subscriptionStatuses", SubscriptionStatus.values());
		model.addAttribute("activeMenu", "subscriptions");
		return "sbadmin/platform/subscriptions/list";
	}

	@GetMapping("/{id}/edit")
	public String editForm(@AuthenticationPrincipal final OidcUser principal, @PathVariable final Long id,
			final Model model, final RedirectAttributes redirectAttributes) {
		requirePlatformAdmin(principal, "subscriptions.edit");
		final Subscription subscription = subscriptionRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("Subscription not found"));
		if (!SubscriptionAdminAccessRules.isEditable(subscription)) {
			redirectAttributes.addFlashAttribute("errorMessage", "Las suscripciones revocadas no pueden editarse.");
			return "redirect:/admin/platform/subscriptions";
		}
		if (!model.containsAttribute("form")) {
			model.addAttribute("form", toForm(subscription));
		}
		model.addAttribute("subscription", subscription);
		model.addAttribute("subscriptionOwner", ownerResolver.resolve(id).orElse(null));
		model.addAttribute("clinicName", clinicRepository.findBySubscriptionId(id).map(Clinic::getName).orElse("—"));
		model.addAttribute("revocableInvitationId", resolveRevocableInvitationId(subscription));
		model.addAttribute("planTierChangeable", isPlanTierChangeable(subscription));
		model.addAttribute("planTiers", PlanTier.values());
		model.addAttribute("subscriptionStatuses", SubscriptionStatus.values());
		model.addAttribute("activeMenu", "subscriptions");
		return "sbadmin/platform/subscriptions/edit";
	}

	@PostMapping("/{id}")
	public String update(@AuthenticationPrincipal final OidcUser principal, @PathVariable final Long id,
			@Valid @ModelAttribute("form") final UpdateSubscriptionForm form, final BindingResult bindingResult,
			final Model model, final RedirectAttributes redirectAttributes) {
		requirePlatformAdmin(principal, "subscriptions.edit");
		final Subscription subscription = subscriptionRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("Subscription not found"));
		if (!SubscriptionAdminAccessRules.isEditable(subscription)) {
			redirectAttributes.addFlashAttribute("errorMessage", "Las suscripciones revocadas no pueden editarse.");
			return "redirect:/admin/platform/subscriptions";
		}
		if (bindingResult.hasErrors()) {
			model.addAttribute("subscription", subscription);
			model.addAttribute("subscriptionOwner", ownerResolver.resolve(id).orElse(null));
			model.addAttribute("clinicName",
					clinicRepository.findBySubscriptionId(id).map(Clinic::getName).orElse("—"));
			model.addAttribute("revocableInvitationId", resolveRevocableInvitationId(subscription));
			model.addAttribute("planTierChangeable", isPlanTierChangeable(subscription));
			model.addAttribute("planTiers", PlanTier.values());
			model.addAttribute("subscriptionStatuses", SubscriptionStatus.values());
			model.addAttribute("activeMenu", "subscriptions");
			return "sbadmin/platform/subscriptions/edit";
		}
		final AdminSubscriptionOverride override = new AdminSubscriptionOverride(form.isPaymentExempt(),
				form.getPeriodEnd(), form.getGracePeriodDays(), form.getStatus(), form.getReasonCode(),
				form.getDetails());
		lifecycleService.applyAdminOverride(principal.getSubject(), id, override);
		redirectAttributes.addFlashAttribute("successMessage", "Suscripción actualizada correctamente.");
		return "redirect:/admin/platform/subscriptions";
	}

	@PostMapping("/{id}/change-plan-tier")
	public String changePlanTier(@AuthenticationPrincipal final OidcUser principal, @PathVariable final Long id,
			@RequestParam final PlanTier planTier, final RedirectAttributes redirectAttributes) {
		requirePlatformAdmin(principal, "subscriptions.change-plan-tier");
		final Subscription subscription = subscriptionRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("Subscription not found"));
		if (!SubscriptionAdminAccessRules.isEditable(subscription)) {
			redirectAttributes.addFlashAttribute("errorMessage", "Las suscripciones revocadas no pueden editarse.");
			return "redirect:/admin/platform/subscriptions";
		}
		try {
			final PlanTierChangeResult result = nutritionistRoleService.changeSubscriptionPlanTier(principal, id,
					planTier);
			if (result.previousTier() == result.newTier()) {
				redirectAttributes.addFlashAttribute("successMessage", "El plan ya es " + planTier + ".");
			}
			else {
				String message = "Plan actualizado de " + result.previousTier() + " a " + result.newTier() + ".";
				if (!result.auth0SyncSucceeded()) {
					message += " Advertencia: no se pudo sincronizar el rol en Auth0; reintente más tarde.";
				}
				redirectAttributes.addFlashAttribute("successMessage", message);
			}
		}
		catch (ResponseStatusException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getReason());
		}
		return "redirect:/admin/platform/subscriptions/" + id + "/edit";
	}

	@PostMapping("/{id}/revoke-access")
	public String revokeAccess(@AuthenticationPrincipal final OidcUser principal, @PathVariable final Long id,
			@RequestParam(required = false) final String reason, final RedirectAttributes redirectAttributes) {
		requirePlatformAdmin(principal, "subscriptions.revoke");
		final Subscription subscription = subscriptionRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("Subscription not found"));
		final NutritionistInvitation invitation = invitationRepository.findBySubscriptionId(id)
			.orElseThrow(() -> new IllegalArgumentException("No invitation linked to subscription"));
		if (!NutritionistInvitationAccessRules.canRevokeAccess(invitation)) {
			throw new IllegalArgumentException("Subscription access cannot be revoked");
		}
		invitationService.revokeNutritionistAccess(principal, invitation.getId(), reason);
		redirectAttributes.addFlashAttribute("successMessage",
				"Acceso revocado para la suscripción #" + subscription.getId() + ".");
		return "redirect:/admin/platform/subscriptions";
	}

	private static UpdateSubscriptionForm toForm(final Subscription subscription) {
		final UpdateSubscriptionForm form = new UpdateSubscriptionForm();
		form.setPaymentExempt(subscription.isPaymentExempt());
		if (subscription.getPeriodEnd() != null) {
			form.setPeriodEndInput(PERIOD_END_INPUT.format(subscription.getPeriodEnd()));
		}
		form.setGracePeriodDays(subscription.getGracePeriodDays());
		form.setStatus(subscription.getStatus());
		return form;
	}

	private Long resolveRevocableInvitationId(final Subscription subscription) {
		return invitationRepository.findBySubscriptionId(subscription.getId())
			.filter(NutritionistInvitationAccessRules::canRevokeAccess)
			.map(NutritionistInvitation::getId)
			.orElse(null);
	}

	private static boolean isPlanTierChangeable(final Subscription subscription) {
		return subscription.getStatus() == SubscriptionStatus.TRIAL
				|| subscription.getStatus() == SubscriptionStatus.ACTIVE
				|| subscription.getStatus() == SubscriptionStatus.GRACE;
	}

}
