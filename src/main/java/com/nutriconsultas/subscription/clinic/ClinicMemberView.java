package com.nutriconsultas.subscription.clinic;

import java.time.Instant;

import com.nutriconsultas.subscription.ClinicMemberRole;
import com.nutriconsultas.subscription.MembershipStatus;

/**
 * Read model for a single row on the director clinic roster.
 */
public record ClinicMemberView(Long memberId, String userId, String displayLabel, ClinicMemberRole role,
		MembershipStatus membershipStatus, Instant joinedAt, boolean currentUser) {
}
