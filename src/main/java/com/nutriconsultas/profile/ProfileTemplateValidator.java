package com.nutriconsultas.profile;

import java.util.Map;

import com.nutriconsultas.subscription.SubscriptionStatus;
import com.nutriconsultas.validation.template.BaseTemplateValidator;

/**
 * Thymeleaf template validator for the nutritionist profile admin pages.
 *
 * <p>
 * Provides mock model variables matching what {@link NutritionistProfileController}
 * places in the model so that template validation succeeds during the test phase.
 */
public class ProfileTemplateValidator extends BaseTemplateValidator {

	@Override
	public String getTemplatePathPattern() {
		return "sbadmin/profile/*";
	}

	@Override
	public Map<String, Object> createMockModelVariables() {
		final Map<String, Object> variables = super.createMockModelVariables();
		final NutritionistProfile mockProfile = createMockProfile();
		variables.put("profile", mockProfile);
		variables.put("subscriptionPlanLabel", "Profesional");
		variables.put("subscriptionStatus", SubscriptionStatus.ACTIVE);
		variables.put("subscriptionPeriodStartLabel", "01/06/2026");
		variables.put("subscriptionPeriodEndLabel", "01/07/2026");
		return variables;
	}

	private NutritionistProfile createMockProfile() {
		final NutritionistProfile profile = new NutritionistProfile();
		profile.setId(1L);
		profile.setUserId("auth0|mock123");
		profile.setDisplayName("Dra. Prueba Validación");
		profile.setCedulaProfesional("12345678");
		profile.setLogoExtension("png");
		return profile;
	}

}
