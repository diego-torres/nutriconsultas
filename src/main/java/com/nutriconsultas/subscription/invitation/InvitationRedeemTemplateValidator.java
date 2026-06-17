package com.nutriconsultas.subscription.invitation;

import java.util.Map;

import com.nutriconsultas.validation.template.BaseTemplateValidator;

public class InvitationRedeemTemplateValidator extends BaseTemplateValidator {

	@Override
	public String getTemplatePathPattern() {
		return "sbadmin/invitation/*";
	}

	@Override
	public Map<String, Object> createMockModelVariables() {
		final Map<String, Object> variables = super.createMockModelVariables();
		variables.put("token", "sample-token");
		return variables;
	}

}
