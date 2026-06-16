package com.nutriconsultas.subscription;

import java.util.Optional;

import org.springframework.lang.NonNull;

/**
 * Resolves effective plan tier and entitlements for a nutritionist user (solo or clinic
 * member).
 */
public interface SubscriptionEntitlementService {

	Optional<PlanTier> getEffectivePlanTier(@NonNull String userId);

	boolean hasEntitlement(@NonNull String userId, @NonNull Entitlement entitlement);

}
