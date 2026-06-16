package com.nutriconsultas.admin;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.nutriconsultas.controller.AbstractPlatformAdminController;
import com.nutriconsultas.platform.PlatformAdminAuthorization;

@Controller
@RequestMapping("/admin/platform")
public class PlatformAdminController extends AbstractPlatformAdminController {

	public PlatformAdminController(final PlatformAdminAuthorization platformAdminAuthorization) {
		super(platformAdminAuthorization);
	}

	@GetMapping
	public String index(@AuthenticationPrincipal final OidcUser principal, final Model model) {
		requirePlatformAdmin(principal, "platform.index");
		model.addAttribute("activeMenu", "platform");
		return "sbadmin/platform/index";
	}

}
