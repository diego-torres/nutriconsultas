package com.nutriconsultas.subscription.clinic;

import org.springframework.lang.NonNull;

/**
 * Clinic director operations: roster visibility and member suspend/reactivate.
 */
public interface ClinicService {

	/**
	 * Loads the clinic roster for the authenticated director. The director must own the
	 * clinic and have {@code USER_ADMINISTRATION} entitlement.
	 */
	ClinicRosterOverview getDirectorRoster(@NonNull String directorUserId);

	/**
	 * Suspends a nutritionist member of the director's clinic. Directors cannot suspend
	 * themselves or other directors.
	 */
	void suspendMember(@NonNull String directorUserId, @NonNull Long memberId);

	/**
	 * Reactivates a suspended nutritionist when a seat is available.
	 */
	void reactivateMember(@NonNull String directorUserId, @NonNull Long memberId);

}
