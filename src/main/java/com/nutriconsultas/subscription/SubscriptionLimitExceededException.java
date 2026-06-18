package com.nutriconsultas.subscription;

/**
 * Thrown when a subscription plan cap blocks an action (patient or nutritionist limit).
 */
public class SubscriptionLimitExceededException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private static final Object[] NO_ARGS = new Object[0];

	private final String messageKey;

	private final Object[] messageArgs;

	public SubscriptionLimitExceededException(final String messageKey) {
		super(messageKey);
		this.messageKey = messageKey;
		this.messageArgs = NO_ARGS;
	}

	public SubscriptionLimitExceededException(final String messageKey, final Object arg) {
		super(messageKey);
		this.messageKey = messageKey;
		this.messageArgs = new Object[] {arg};
	}

	public String getMessageKey() {
		return messageKey;
	}

	public Object[] getMessageArgs() {
		return messageArgs.clone();
	}

}
