package com.nutriconsultas.subscription;

/**
 * Thrown when a subscription plan cap blocks an action (patient or nutritionist limit).
 */
public class SubscriptionLimitExceededException extends RuntimeException {

	private final String messageKey;

	private final transient Object[] messageArgs;

	public SubscriptionLimitExceededException(final String messageKey) {
		this(messageKey, null);
	}

	public SubscriptionLimitExceededException(final String messageKey, final Object... messageArgs) {
		super(messageKey);
		this.messageKey = messageKey;
		this.messageArgs = messageArgs;
	}

	public String getMessageKey() {
		return messageKey;
	}

	public Object[] getMessageArgs() {
		return messageArgs;
	}

}
