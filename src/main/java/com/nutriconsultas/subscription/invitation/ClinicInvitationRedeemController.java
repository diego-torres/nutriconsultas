package com.nutriconsultas.subscription.invitation;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.nutriconsultas.controller.AbstractAuthorizedController;
import com.nutriconsultas.security.NutritionistOAuth2LoginSuccessHandler;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/invitation/clinic")
public class ClinicInvitationRedeemController extends AbstractAuthorizedController {

	private final ClinicInvitationService clinicInvitationService;

	public ClinicInvitationRedeemController(final ClinicInvitationService clinicInvitationService) {
		this.clinicInvitationService = clinicInvitationService;
	}

	@GetMapping("/redeem")
	public String redeemPage(@RequestParam(name = "token", required = false) final String token,
			@AuthenticationPrincipal final OidcUser principal, final HttpSession session, final Model model) {
		if (StringUtils.hasText(token)) {
			session.setAttribute(NutritionistOAuth2LoginSuccessHandler.PENDING_CLINIC_INVITATION_TOKEN_SESSION_KEY,
					token);
		}
		model.addAttribute("token", token);
		model.addAttribute("authenticated", principal != null);
		return "sbadmin/invitation/clinic-redeem";
	}

	@PostMapping("/redeem")
	public String redeem(@RequestParam("token") final String token, @AuthenticationPrincipal final OidcUser principal) {
		clinicInvitationService.redeemInvitation(principal, token);
		return "redirect:/admin";
	}

}
