package com.nutriconsultas.platform;

import java.util.Map;

import com.nutriconsultas.validation.template.BaseTemplateValidator;

public class PlatformAdminTemplateValidator extends BaseTemplateValidator {

	@Override
	public String getTemplatePathPattern() {
		return "sbadmin/platform/*";
	}

	@Override
	public Map<String, Object> createMockModelVariables() {
		final Map<String, Object> variables = super.createMockModelVariables();
		variables.put("platformAdmin", true);
		variables.put("activeMenu", "platform");
		return variables;
	}

}
