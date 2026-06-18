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

	/**
	 * Verifies the user may create a new patient (entitlement, grace policy, and plan
	 * cap).
	 * @throws SubscriptionLimitExceededException when blocked
	 */
	void assertCanCreatePatient(@NonNull String userId);

	/**
	 * Verifies a clinic director may invite another nutritionist (role, seats, grace
	 * policy).
	 * @throws SubscriptionLimitExceededException when blocked
	 */
	void assertCanInviteNutritionist(@NonNull String directorUserId);

}
