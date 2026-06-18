package com.nutriconsultas.platform;

import java.util.Map;

import com.nutriconsultas.admin.CreateNutritionistInvitationForm;
import com.nutriconsultas.admin.UpdateSubscriptionForm;
import com.nutriconsultas.subscription.InvitationStatus;
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
		variables.put("highlightInvitationId", null);
		variables.put("errorMessage", null);
		variables.put("conflictingInvitationId", null);
		variables.put("planTiers", PlanTier.values());
		variables.put("invitationStatuses", InvitationStatus.values());
		variables.put("form", new CreateNutritionistInvitationForm());
		variables.put("subscriptionStatuses", com.nutriconsultas.subscription.SubscriptionStatus.values());
		variables.put("subscription", createMockSubscription());
		variables.put("clinicName", "Consultorio demo");
		variables.put("revocableInvitationId", 1L);
		variables.put("subscriptionBanner", null);
		variables.put("subscriptionStatus", com.nutriconsultas.subscription.SubscriptionStatus.SUSPENDED);
		variables.put("updateSubscriptionForm", new UpdateSubscriptionForm());
		return variables;
	}

	private static com.nutriconsultas.subscription.Subscription createMockSubscription() {
		final com.nutriconsultas.subscription.Subscription subscription = new com.nutriconsultas.subscription.Subscription();
		subscription.setId(1L);
		subscription.setPlanTier(PlanTier.BASICO);
		subscription.setStatus(com.nutriconsultas.subscription.SubscriptionStatus.ACTIVE);
		return subscription;
	}

}
