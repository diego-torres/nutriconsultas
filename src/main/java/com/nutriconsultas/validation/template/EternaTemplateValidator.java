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
		// Public templates may not need username/user_picture, but we include them
		// for consistency
		return super.createMockModelVariables();
	}

}
