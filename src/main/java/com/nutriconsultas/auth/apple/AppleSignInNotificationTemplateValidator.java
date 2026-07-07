package com.nutriconsultas.auth.apple;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.nutriconsultas.validation.template.BaseTemplateValidator;

public class AppleSignInNotificationTemplateValidator extends BaseTemplateValidator {

	@Override
	public String getTemplatePathPattern() {
		return "sbadmin/platform/apple-signin/*";
	}

	@Override
	public Map<String, Object> createMockModelVariables() {
		final Map<String, Object> variables = super.createMockModelVariables();
		variables.put("platformAdmin", true);
		variables.put("notifications", sampleNotifications());
		variables.put("activeMenu", "apple-signin");
		return variables;
	}

	private static List<AppleSignInNotification> sampleNotifications() {
		final List<AppleSignInNotification> notifications = new ArrayList<>();
		final AppleSignInNotification consentRevoked = new AppleSignInNotification();
		consentRevoked.setId(1L);
		consentRevoked.setAppleEventId("evt-consent-1");
		consentRevoked.setEventType(AppleSignInEventType.CONSENT_REVOKED);
		consentRevoked.setPacienteId(42L);
		consentRevoked.setIdentityMappingStatus(AppleIdentityMappingStatus.MAPPED);
		consentRevoked.setLifecycleAction(AppleSignInLifecycleAction.APPLIED_ACCESS_REVOKED);
		consentRevoked.setProcessingStatus(AppleSignInNotificationProcessingStatus.PROCESSED);
		consentRevoked.setReceivedAt(Instant.parse("2026-07-07T12:00:00Z"));
		notifications.add(consentRevoked);
		final AppleSignInNotification accountDelete = new AppleSignInNotification();
		accountDelete.setId(2L);
		accountDelete.setAppleEventId("evt-delete-1");
		accountDelete.setEventType(AppleSignInEventType.ACCOUNT_DELETE);
		accountDelete.setPacienteId(99L);
		accountDelete.setIdentityMappingStatus(AppleIdentityMappingStatus.NO_LOCAL_USER);
		accountDelete.setLifecycleAction(AppleSignInLifecycleAction.SKIPPED_NO_PACIENTE);
		accountDelete.setProcessingStatus(AppleSignInNotificationProcessingStatus.PROCESSED);
		accountDelete.setReceivedAt(Instant.parse("2026-07-06T10:00:00Z"));
		notifications.add(accountDelete);
		return notifications;
	}

}
