package com.nutriconsultas.auth0;

import com.nutriconsultas.subscription.PlanTier;

/**
 * Syncs nutritionist plan roles to Auth0 via the Management API. DB
 * {@code Subscription.planTier} remains authoritative; Auth0 roles are a cache for
 * JWT/session claims.
 */
public interface Auth0RoleSyncClient {

	boolean isConfigured();

	/**
	 * Removes all plan-tier Auth0 roles from the user and assigns the role for
	 * {@code planTier}.
	 */
	void syncPlanRole(String auth0UserId, PlanTier planTier);

}
