package com.nutriconsultas.subscription.invitation;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.nutriconsultas.controller.AbstractAuthorizedController;

@Controller
@RequestMapping("/invitation/nutritionist")
public class NutritionistInvitationRedeemController extends AbstractAuthorizedController {

	private final NutritionistInvitationService invitationService;

	private final NutritionistInvitationDevCheckoutService devCheckoutService;

	public NutritionistInvitationRedeemController(final NutritionistInvitationService invitationService,
			final NutritionistInvitationDevCheckoutService devCheckoutService) {
		this.invitationService = invitationService;
		this.devCheckoutService = devCheckoutService;
	}

	@GetMapping("/redeem")
	public String redeemPage(@RequestParam(name = "token", required = false) final String token, final Model model) {
		model.addAttribute("token", token);
		return "sbadmin/invitation/redeem";
	}

	@PostMapping("/redeem")
	public String redeem(@RequestParam("token") final String token, @AuthenticationPrincipal final OidcUser principal) {
		final RedeemNutritionistInvitationResult result = invitationService.redeemInvitation(principal, token);
		if (result instanceof RedeemNutritionistInvitationResult.CheckoutRedirect checkout) {
			return "redirect:" + checkout.checkoutUrl();
		}
		return "redirect:/admin";
	}

	@GetMapping("/dev-checkout")
	public String completeDevCheckout(@RequestParam("invitationId") final Long invitationId,
			@AuthenticationPrincipal final OidcUser principal) {
		devCheckoutService.completeStubCheckout(principal, invitationId);
		return "redirect:/admin";
	}

}
