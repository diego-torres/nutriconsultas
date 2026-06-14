package com.nutriconsultas.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.nutriconsultas.platform.PlatformAdminService;

@Component
@ControllerAdvice
public class PlatformAdminModelAdvice {

	private final PlatformAdminService platformAdminService;

	public PlatformAdminModelAdvice(final PlatformAdminService platformAdminService) {
		this.platformAdminService = platformAdminService;
	}

	@ModelAttribute
	public void addPlatformAdminFlag(@AuthenticationPrincipal final OidcUser principal, final Model model) {
		final boolean platformAdmin = platformAdminService.isPlatformAdmin(principal);
		model.addAttribute("platformAdmin", platformAdmin);
	}

}
