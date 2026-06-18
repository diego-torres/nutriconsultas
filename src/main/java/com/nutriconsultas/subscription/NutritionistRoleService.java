package com.nutriconsultas.subscription;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Assigns nutritionist plan tiers. DB {@link Subscription#planTier} is authoritative;
 * Auth0 roles are synced as a cache for JWT/session claims.
 */
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface NutritionistRoleService {

	void assignRole(OidcUser adminPrincipal, String targetUserId, PlanTier planTier);

	/**
	 * Changes {@link Subscription#planTier} for an active subscription, syncs Auth0, and
	 * records a platform-admin audit event. Blocks downgrades when clinic usage exceeds
	 * the new plan limits.
	 */
	PlanTierChangeResult changeSubscriptionPlanTier(OidcUser adminPrincipal, Long subscriptionId, PlanTier newTier);

}
