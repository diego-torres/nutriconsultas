package com.nutriconsultas.paciente;

/**
 * Patient invitation token lifecycle (#132). Distinct from subscription
 * {@link com.nutriconsultas.subscription.InvitationStatus}.
 */
public enum PatientInvitationStatus {

	PENDING, REDEEMED, EXPIRED, REVOKED

}
