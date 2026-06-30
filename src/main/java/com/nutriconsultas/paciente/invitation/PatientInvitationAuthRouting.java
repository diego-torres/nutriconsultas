package com.nutriconsultas.paciente.invitation;

import org.springframework.util.StringUtils;

import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteStatus;
import com.nutriconsultas.util.EmailMasking;

/**
 * Resolves invitation preview auth routing hints for the mobile app (#349).
 */
public final class PatientInvitationAuthRouting {

	private PatientInvitationAuthRouting() {
	}

	public static boolean isMobileAppLinked(final Paciente paciente) {
		return paciente != null && StringUtils.hasText(paciente.getPatientAuthSub());
	}

	public static InvitationAuthPath resolveAuthPath(final PacienteStatus status, final boolean mobileAppLinked) {
		if (status == PacienteStatus.INVITED && !mobileAppLinked) {
			return InvitationAuthPath.CREATE_ACCOUNT;
		}
		return InvitationAuthPath.SIGN_IN;
	}

	public static String resolveEmailHint(final Paciente paciente) {
		final String recipientEmail = PatientMobileInvitationUiSupport.resolveRecipientEmail(paciente);
		return EmailMasking.maskForHint(recipientEmail);
	}

}
