package com.nutriconsultas.ai;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.nutriconsultas.subscription.Entitlement;
import com.nutriconsultas.subscription.SubscriptionEntitlementService;

/**
 * Plan-tier gate for the AI nutrition assistant (#409). {@code AI_ENABLED=true} alone does
 * not grant access — Plus and Consultorio only.
 */
@Component
public final class AiEntitlementGuard {

	private final SubscriptionEntitlementService subscriptionEntitlementService;

	public AiEntitlementGuard(final SubscriptionEntitlementService subscriptionEntitlementService) {
		this.subscriptionEntitlementService = subscriptionEntitlementService;
	}

	public void assertCanUseAiAssistant(@Nullable final String nutritionistId) {
		if (!StringUtils.hasText(nutritionistId)) {
			subscriptionEntitlementService.assertCanUseAiAssistant("");
			return;
		}
		subscriptionEntitlementService.assertCanUseAiAssistant(nutritionistId);
	}

	public boolean canUseAiAssistant(@Nullable final String nutritionistId) {
		if (!StringUtils.hasText(nutritionistId)) {
			return false;
		}
		return subscriptionEntitlementService.hasEntitlement(nutritionistId, Entitlement.AI_ASSISTANT);
	}

}
