package com.nutriconsultas.subscription;

/**
 * Subscription lifecycle states. See
 * {@code docs/subscription/SUBSCRIPTION-ENFORCEMENT-PLAN.md}.
 */
public enum SubscriptionStatus {

	PENDING_PAYMENT, TRIAL, ACTIVE, GRACE, SUSPENDED, CANCELLED

}
