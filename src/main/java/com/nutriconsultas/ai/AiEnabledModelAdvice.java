package com.nutriconsultas.ai;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Exposes the AI feature flag to admin templates (#388). Never adds API keys or OpenAI
 * configuration — only {@code aiEnabled} from {@link AiProperties#isEnabled()} and plan
 * entitlement via {@code aiAssistantAvailable} (#409).
 */
@Component
@ControllerAdvice
public class AiEnabledModelAdvice {

	private final AiProperties aiProperties;

	private final AiEntitlementGuard aiEntitlementGuard;

	public AiEnabledModelAdvice(final AiProperties aiProperties, final AiEntitlementGuard aiEntitlementGuard) {
		this.aiProperties = aiProperties;
		this.aiEntitlementGuard = aiEntitlementGuard;
	}

	@ModelAttribute
	public void addAiEnabledFlag(final Model model, @AuthenticationPrincipal final OidcUser principal) {
		final boolean serverEnabled = aiProperties.isEnabled();
		model.addAttribute("aiEnabled", serverEnabled);
		final String userId = principal != null ? principal.getSubject() : null;
		model.addAttribute("aiAssistantAvailable", serverEnabled && aiEntitlementGuard.canUseAiAssistant(userId));
	}

}
