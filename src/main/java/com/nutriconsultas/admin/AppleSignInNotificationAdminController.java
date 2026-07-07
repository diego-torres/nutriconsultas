package com.nutriconsultas.admin;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.nutriconsultas.auth.apple.AppleSignInNotificationAdminService;
import com.nutriconsultas.controller.AbstractPlatformAdminController;
import com.nutriconsultas.platform.PlatformAdminAuthorization;

@Controller
@RequestMapping("/admin/platform/apple-signin")
public class AppleSignInNotificationAdminController extends AbstractPlatformAdminController {

	private final AppleSignInNotificationAdminService notificationAdminService;

	public AppleSignInNotificationAdminController(final AppleSignInNotificationAdminService notificationAdminService,
			final PlatformAdminAuthorization platformAdminAuthorization) {
		super(platformAdminAuthorization);
		this.notificationAdminService = notificationAdminService;
	}

	@GetMapping
	public String listDestructiveEvents(@AuthenticationPrincipal final OidcUser principal, final Model model) {
		requirePlatformAdmin(principal, "apple-signin.list");
		model.addAttribute("notifications", notificationAdminService.findDestructiveEventsNewestFirst());
		model.addAttribute("activeMenu", "apple-signin");
		return "sbadmin/platform/apple-signin/listado";
	}

}
