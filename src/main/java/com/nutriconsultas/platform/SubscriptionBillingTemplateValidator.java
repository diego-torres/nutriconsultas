package com.nutriconsultas.platform;

import java.util.Map;

import com.nutriconsultas.subscription.SubscriptionStatus;
import com.nutriconsultas.validation.template.BaseTemplateValidator;

public class SubscriptionBillingTemplateValidator extends BaseTemplateValidator {

	@Override
	public String getTemplatePathPattern() {
		return "sbadmin/subscription/**";
	}

	@Override
	public Map<String, Object> createMockModelVariables() {
		final Map<String, Object> variables = super.createMockModelVariables();
		variables.put("subscriptionStatus", SubscriptionStatus.SUSPENDED);
		variables.put("periodEnd", null);
		variables.put("activeMenu", "home");
		return variables;
	}

}
