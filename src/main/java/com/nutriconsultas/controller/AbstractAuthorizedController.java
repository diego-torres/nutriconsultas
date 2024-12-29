package com.nutriconsultas.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractAuthorizedController {
    @ModelAttribute
    public void addAttributes(@AuthenticationPrincipal OidcUser principal, Model model) {
        log.debug("Adding attributes to model");
        if (principal != null) {
            model.addAttribute("username", principal.getClaims().get("name"));
            log.debug("Adding username to model: {}", principal.getClaims().get("name"));
            model.addAttribute("user_picture", principal.getClaims().get("picture"));
            log.debug("Adding user_picture to model: {}", principal.getClaims().get("picture"));
        }
        log.debug("Finishing adding attributes to model");
    }
}