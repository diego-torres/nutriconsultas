package com.nutriconsultas.subscription.clinic;

import java.util.List;

import com.nutriconsultas.subscription.PlanTier;
import com.nutriconsultas.subscription.SubscriptionStatus;

/**
 * Director dashboard: clinic identity, seat usage, and member roster.
 */
public record ClinicRosterOverview(Long clinicId, String clinicName, PlanTier planTier,
		SubscriptionStatus subscriptionStatus, int maxNutritionists, long activeSeatCount, long pendingInviteCount,
		List<ClinicMemberView> members, List<ClinicInvitationView> pendingInvitations) {

	public boolean canInviteMore() {
		return activeSeatCount + pendingInviteCount < maxNutritionists;
	}

}
