package com.nutriconsultas.platform;

import java.util.List;
import java.util.Map;

import com.nutriconsultas.admin.CreateNutritionistInvitationForm;
import com.nutriconsultas.subscription.PlanTier;
import com.nutriconsultas.validation.template.BaseTemplateValidator;

public class PlatformAdminTemplateValidator extends BaseTemplateValidator {

	@Override
	public String getTemplatePathPattern() {
		return "sbadmin/platform/**";
	}

	@Override
	public Map<String, Object> createMockModelVariables() {
		final Map<String, Object> variables = super.createMockModelVariables();
		variables.put("platformAdmin", true);
		variables.put("activeMenu", "platform");
		variables.put("invitations", List.of());
		variables.put("planTiers", PlanTier.values());
		variables.put("form", new CreateNutritionistInvitationForm());
		return variables;
	}

}
