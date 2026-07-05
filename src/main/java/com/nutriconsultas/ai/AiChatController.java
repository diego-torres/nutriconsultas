package com.nutriconsultas.ai;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.nutriconsultas.controller.AbstractAuthorizedController;
import com.nutriconsultas.subscription.SubscriptionErrorResponses;
import com.nutriconsultas.subscription.SubscriptionLimitExceededException;

/**
 * Nutritionist web AI chat page (#388, #389).
 */
@Controller
@RequestMapping("/admin/ai")
public class AiChatController extends AbstractAuthorizedController {

	private final AiProperties aiProperties;

	private final AiEntitlementGuard aiEntitlementGuard;

	private final SubscriptionErrorResponses subscriptionErrorResponses;

	public AiChatController(final AiProperties aiProperties, final AiEntitlementGuard aiEntitlementGuard,
			final SubscriptionErrorResponses subscriptionErrorResponses) {
		this.aiProperties = aiProperties;
		this.aiEntitlementGuard = aiEntitlementGuard;
		this.subscriptionErrorResponses = subscriptionErrorResponses;
	}

	@GetMapping
	public String chatHome(@RequestParam(name = "threadId", required = false) final Long threadId, final Model model,
			@AuthenticationPrincipal final OidcUser principal) {
		if (!aiProperties.isEnabled()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		assertAiAssistantEntitlement(principal);
		model.addAttribute("activeMenu", "ai");
		model.addAttribute("initialThreadId", threadId);
		return "sbadmin/ai/chat";
	}

	private void assertAiAssistantEntitlement(final OidcUser principal) {
		final String userId = principal != null ? principal.getSubject() : null;
		try {
			aiEntitlementGuard.assertCanUseAiAssistant(userId);
		}
		catch (final SubscriptionLimitExceededException ex) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, subscriptionErrorResponses.resolve(ex));
		}
	}

}
