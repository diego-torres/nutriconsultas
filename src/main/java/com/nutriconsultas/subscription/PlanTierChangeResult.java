package com.nutriconsultas.subscription;

/**
 * Outcome of a platform-admin subscription plan tier change (#211).
 */
public record PlanTierChangeResult(PlanTier previousTier, PlanTier newTier, boolean auth0SyncSucceeded) {

}
