package com.nutriconsultas.paciente.invitation;

import org.springframework.util.StringUtils;

import com.nutriconsultas.paciente.Paciente;
import com.nutriconsultas.paciente.PacienteStatus;
import com.nutriconsultas.paciente.PatientInvitation;
import com.nutriconsultas.paciente.projection.PacienteListView;
import com.nutriconsultas.util.LogRedaction;

/**
 * Resolves nutritionist-web mobile invitation UI state (#341).
 */
public final class PatientMobileInvitationUiSupport {

	private PatientMobileInvitationUiSupport() {
	}

	public static PatientMobileInvitationStatus resolve(final Paciente paciente,
			final PatientInvitation pendingInvitation) {
		if (paciente.getStatus() == PacienteStatus.REVOKED) {
			return status("REVOKED", "Acceso revocado", false, false, false, null, null, null, null);
		}
		if (StringUtils.hasText(paciente.getPatientAuthSub())) {
			if (paciente.getStatus() == PacienteStatus.ONBOARDING) {
				return status("ONBOARDING", "Registrándose en la app", false, false, false, null, null, null, null);
			}
			return status("LINKED", "Vinculado a la app", false, false, false, null, null, null, null);
		}
		if (paciente.getStatus() == PacienteStatus.INVITED && pendingInvitation != null) {
			return status("PENDING", "Invitación pendiente", false, true, true, pendingInvitation.getId(),
					pendingInvitation.getHumanCode(), pendingInvitation.getExpiresAt(), redactRecipientEmail(paciente));
		}
		if (!StringUtils.hasText(resolveRecipientEmail(paciente))) {
			return status("NO_EMAIL", "Sin correo registrado", false, false, false, null, null, null, null);
		}
		return status("NONE", "Sin app", true, false, false, null, null, null, redactRecipientEmail(paciente));
	}

	public static String resolveRecipientEmail(final Paciente paciente) {
		if (StringUtils.hasText(paciente.getEmail())) {
			return paciente.getEmail().trim().toLowerCase();
		}
		if (StringUtils.hasText(paciente.getEmailHint())) {
			return paciente.getEmailHint().trim().toLowerCase();
		}
		return null;
	}

	public static String resolveRecipientEmailFromListRow(final PacienteListView row) {
		if (row == null) {
			return null;
		}
		if (StringUtils.hasText(row.getEmail())) {
			return row.getEmail().trim().toLowerCase();
		}
		return null;
	}

	public static String gridBadgeHtml(final PacienteStatus status, final String patientAuthSub) {
		if (status == PacienteStatus.REVOKED) {
			return badge("badge-dark", "Revocado");
		}
		if (StringUtils.hasText(patientAuthSub)) {
			if (status == PacienteStatus.ONBOARDING) {
				return badge("badge-info", "En onboarding");
			}
			return badge("badge-success", "Vinculado");
		}
		if (status == PacienteStatus.INVITED) {
			return badge("badge-warning", "Invitación pendiente");
		}
		return badge("badge-secondary", "Sin app");
	}

	public static boolean canInviteFromGrid(final PacienteStatus status, final String patientAuthSub,
			final String email) {
		if (status == PacienteStatus.REVOKED || StringUtils.hasText(patientAuthSub)) {
			return false;
		}
		if (status == PacienteStatus.ONBOARDING) {
			return false;
		}
		if (status == PacienteStatus.INVITED) {
			return StringUtils.hasText(email);
		}
		return status == PacienteStatus.ACTIVE && StringUtils.hasText(email);
	}

	private static PatientMobileInvitationStatus status(final String stateCode, final String stateLabel,
			final boolean canSend, final boolean canResend, final boolean canRevoke, final Long pendingInvitationId,
			final String humanCode, final java.time.Instant expiresAt, final String recipientEmailRedacted) {
		return new PatientMobileInvitationStatus(stateCode, stateLabel, canSend, canResend, canRevoke,
				pendingInvitationId, humanCode, expiresAt, recipientEmailRedacted);
	}

	private static String redactRecipientEmail(final Paciente paciente) {
		final String email = resolveRecipientEmail(paciente);
		return email != null ? LogRedaction.redactEmail(email) : null;
	}

	private static String badge(final String cssClass, final String label) {
		return "<span class='badge " + cssClass + "'>" + label + "</span>";
	}

}
