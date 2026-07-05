package com.nutriconsultas.ai;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Combines nutritionist entitlement and user-message validation for AI chat (#409).
 */
@Component
public final class AiChatRequestGuards {

	private final AiUserMessageGuard userMessageGuard;

	private final AiEntitlementGuard aiEntitlementGuard;

	public AiChatRequestGuards(final AiUserMessageGuard userMessageGuard, final AiEntitlementGuard aiEntitlementGuard) {
		this.userMessageGuard = userMessageGuard;
		this.aiEntitlementGuard = aiEntitlementGuard;
	}

	public void assertNutritionistAccess(@NonNull final String nutritionistId) {
		if (!org.springframework.util.StringUtils.hasText(nutritionistId)) {
			throw new AiChatException(org.springframework.http.HttpStatus.UNAUTHORIZED, AiToolErrorCode.VALIDATION,
					"Sesión no válida.");
		}
		aiEntitlementGuard.assertCanUseAiAssistant(nutritionistId);
	}

	public String validateUserMessage(final String userMessage) {
		return userMessageGuard.validateAndSanitize(userMessage);
	}

}
