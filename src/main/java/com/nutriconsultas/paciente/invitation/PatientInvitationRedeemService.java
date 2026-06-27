package com.nutriconsultas.paciente.invitation;

/**
 * Authoritative patient invitation redemption gate (#136).
 */
public interface PatientInvitationRedeemService {

	PatientInvitationRedeemResult redeem(String rawUrlToken, String patientAuthSub);

	PatientInvitationRedeemResult redeemByHumanCode(String humanCode, String patientAuthSub);

}
