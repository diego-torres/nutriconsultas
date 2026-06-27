package com.nutriconsultas.paciente.invitation;

/**
 * Authoritative patient invitation redemption gate (#136).
 */
public interface PatientInvitationRedeemService {

	PatientInvitationRedeemResult redeem(String rawUrlToken, String patientAuthSub);

	PatientInvitationRedeemResult redeemByHumanCode(String humanCode, String patientAuthSub);

	/**
	 * Links an authenticated patient JWT to a pending invitation when redeem did not run
	 * (e.g. returning Auth0 user without stored credential). Matches JWT email to INVITED
	 * Paciente with a non-expired pending invitation, or repairs linkage from a prior
	 * redeem.
	 */
	PatientInvitationRedeemResult reconcile(String patientAuthSub, String email, String rawUrlToken, String humanCode);

}
