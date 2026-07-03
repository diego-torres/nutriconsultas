package com.nutriconsultas.ai;

import java.util.Map;

import com.nutriconsultas.validation.template.BaseTemplateValidator;

public class AiChatTemplateValidator extends BaseTemplateValidator {

	@Override
	public String getTemplatePathPattern() {
		return "sbadmin/ai/**";
	}

	@Override
	public Map<String, Object> createMockModelVariables() {
		final Map<String, Object> variables = super.createMockModelVariables();
		variables.put("aiEnabled", true);
		variables.put("platformAdmin", false);
		variables.put("clinicDirector", false);
		variables.put("activeMenu", "ai");
		variables.put("initialThreadId", null);
		return variables;
	}

}
