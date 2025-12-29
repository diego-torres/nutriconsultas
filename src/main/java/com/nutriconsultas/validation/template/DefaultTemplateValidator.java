package com.nutriconsultas.validation.template;

import java.util.Map;

/**
 * Default validator for templates that don't have a specific validator. This provides
 * only the base common mock variables.
 */
public class DefaultTemplateValidator extends BaseTemplateValidator {

	@Override
	public String getTemplatePathPattern() {
		return "*";
	}

	@Override
	public Map<String, Object> createMockModelVariables() {
		// Return only base variables for templates without specific validators
		return super.createMockModelVariables();
	}

}
