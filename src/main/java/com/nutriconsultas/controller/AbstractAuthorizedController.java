package com.nutriconsultas.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;

public abstract class AbstractAuthorizedController {
    @ModelAttribute
    public void addAttributes(@AuthenticationPrincipal OidcUser principal, Model model) {
        if (principal != null) {
            model.addAttribute("username", principal.getClaims().get("name"));
            model.addAttribute("user_picture", principal.getClaims().get("picture"));
        }

    }
}