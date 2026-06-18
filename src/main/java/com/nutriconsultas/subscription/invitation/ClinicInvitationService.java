package com.nutriconsultas.subscription.invitation;

import org.springframework.lang.NonNull;

import com.nutriconsultas.subscription.SubscriptionEntitlementService;
import com.nutriconsultas.subscription.SubscriptionLimitExceededException;

import org.springframework.stereotype.Service;

/**
 * Clinic director invitations (no payment). Full create/redeem flow lands in #188;
 * capacity checks are enforced here for the invite path (#190).
 */
@Service
public class ClinicInvitationService {

	private final SubscriptionEntitlementService subscriptionEntitlementService;

	public ClinicInvitationService(final SubscriptionEntitlementService subscriptionEntitlementService) {
		this.subscriptionEntitlementService = subscriptionEntitlementService;
	}

	/**
	 * Validates the director may invite another nutritionist before persisting a
	 * {@link com.nutriconsultas.subscription.ClinicInvitation}.
	 * @throws SubscriptionLimitExceededException when the plan seat cap is reached
	 */
	public void assertCanInviteNutritionist(@NonNull final String directorUserId) {
		subscriptionEntitlementService.assertCanInviteNutritionist(directorUserId);
	}

}
