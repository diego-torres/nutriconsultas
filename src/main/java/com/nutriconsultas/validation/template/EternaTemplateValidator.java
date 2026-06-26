package com.nutriconsultas.validation.template;

import java.util.Map;

/**
 * Validator for eterna (public) templates. These templates typically don't require
 * authentication-specific variables.
 */
public class EternaTemplateValidator extends BaseTemplateValidator {

	@Override
	public String getTemplatePathPattern() {
		return "eterna/*";
	}

	@Override
	public Map<String, Object> createMockModelVariables() {
		final Map<String, Object> variables = super.createMockModelVariables();
		variables.put("recaptchaSiteKey", "6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI");
		variables.put("nutritionistDisplayName", "Dra. Ejemplo");
		variables.put("publicBookingId", "00000000-0000-4000-8000-000000000001");
		variables.put("minAdvanceDays", 2);
		variables.put("minBookableDate", "2026-06-22");
		variables.put("advanceNotice", "Las citas requieren al menos 2 días de anticipación.");
		variables.put("valid", true);
		variables.put("rateLimited", false);
		variables.put("inviterDisplayName", "Dra. Ejemplo");
		variables.put("humanCode", "NUTRI-7F3K-9Q2X");
		variables.put("inviteUrl", "http://localhost:3000/links/i/mock-token");
		variables.put("platform", "OTHER");
		variables.put("iosAppStoreUrl", "https://apps.apple.com/app/example");
		variables.put("androidPlayStoreUrl", "https://play.google.com/store/apps/details?id=example");
		return variables;
	}

}
