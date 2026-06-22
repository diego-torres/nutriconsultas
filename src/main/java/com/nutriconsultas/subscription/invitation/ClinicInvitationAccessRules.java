package com.nutriconsultas.subscription.invitation;

import com.nutriconsultas.subscription.ClinicInvitation;
import com.nutriconsultas.subscription.ClinicMember;
import com.nutriconsultas.subscription.InvitationStatus;
import com.nutriconsultas.subscription.MembershipStatus;

/**
 * Shared rules for clinic director invitation eligibility.
 */
public final class ClinicInvitationAccessRules {

	private ClinicInvitationAccessRules() {
	}

	public static boolean blocksNewInvitation(final ClinicInvitation invitation, final ClinicMember member) {
		return invitation != null && invitation.getStatus() == InvitationStatus.REDEEMED && member != null
				&& member.getMembershipStatus() == MembershipStatus.ACTIVE;
	}

}
