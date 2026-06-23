package com.nutriconsultas.paciente.invitation;

/**
 * Authoritative patient invitation redemption gate (#136).
 */
@FunctionalInterface
public interface PatientInvitationRedeemService {

	PatientInvitationRedeemResult redeem(String rawUrlToken, String patientAuthSub);

}
