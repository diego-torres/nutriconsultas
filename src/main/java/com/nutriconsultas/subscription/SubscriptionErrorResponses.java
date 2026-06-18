package com.nutriconsultas.subscription;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * Localized subscription limit and entitlement error messages (#190).
 */
@Component
public final class SubscriptionErrorResponses {

	public static final String KEY_PATIENT_LIMIT = "error.subscription.patient_limit";

	public static final String KEY_CREATE_PATIENT_DENIED = "error.subscription.create_patient_denied";

	public static final String KEY_NUTRITIONIST_LIMIT = "error.subscription.nutritionist_limit";

	public static final String KEY_NUTRITIONIST_INVITE_DENIED = "error.subscription.nutritionist_invite_denied";

	private final MessageSource messageSource;

	public SubscriptionErrorResponses(final MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public String resolve(final SubscriptionLimitExceededException ex, final Locale locale) {
		return messageSource.getMessage(ex.getMessageKey(), ex.getMessageArgs(), locale);
	}

	public String resolve(final SubscriptionLimitExceededException ex) {
		return resolve(ex, Locale.getDefault());
	}

}
