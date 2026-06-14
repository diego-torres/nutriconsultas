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
		return variables;
	}

}
