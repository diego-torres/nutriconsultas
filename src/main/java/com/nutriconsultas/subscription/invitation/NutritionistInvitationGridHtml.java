package com.nutriconsultas.subscription.invitation;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.web.util.HtmlUtils;

import com.nutriconsultas.subscription.InvitationStatus;
import com.nutriconsultas.subscription.NutritionistInvitation;

public final class NutritionistInvitationGridHtml {

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
		.withZone(ZoneId.systemDefault());

	private NutritionistInvitationGridHtml() {
	}

	public static String formatCreatedAt(final NutritionistInvitation invitation) {
		if (invitation.getCreatedAt() == null) {
			return "";
		}
		return DATE_TIME_FORMATTER.format(invitation.getCreatedAt());
	}

	public static String formatExpiresAt(final NutritionistInvitation invitation) {
		if (invitation.getExpiresAt() == null) {
			return "";
		}
		return DATE_TIME_FORMATTER.format(invitation.getExpiresAt());
	}

	public static String statusBadge(final InvitationStatus status) {
		if (status == null) {
			return "<span class=\"badge badge-light\">—</span>";
		}
		return switch (status) {
			case PENDING -> "<span class=\"badge badge-warning\">Pendiente</span>";
			case REDEEMED -> "<span class=\"badge badge-success\">Aceptada</span>";
			case EXPIRED -> "<span class=\"badge badge-secondary\">Expirada</span>";
			case CANCELLED -> "<span class=\"badge badge-dark\">Cancelada</span>";
		};
	}

	public static String paymentExemptBadge(final boolean paymentExempt) {
		if (paymentExempt) {
			return "<span class=\"badge badge-info\">Sí</span>";
		}
		return "<span class=\"badge badge-secondary\">No</span>";
	}

	public static String actionsHtml(final NutritionistInvitation invitation) {
		if (invitation.getStatus() != InvitationStatus.PENDING) {
			return "<span class=\"text-muted small\">—</span>";
		}
		final long id = invitation.getId();
		return """
				<form action='/admin/platform/invitations/%d/regenerate-link' method='post' class='d-inline regenerate-invitation-link-form'>\
				<button type='button' class='btn btn-sm btn-outline-primary regenerate-invitation-link-btn' title='Generar y ver enlace de invitación'>\
				<i class='fas fa-link'></i> Enlace</button></form>\
				<form action='/admin/platform/invitations/%d/cancel' method='post' class='d-inline cancel-invitation-form'>\
				<button type='button' class='btn btn-sm btn-outline-danger cancel-invitation-btn' title='Cancelar invitación'>\
				<i class='fas fa-times'></i> Cancelar</button></form>\
				"""
			.formatted(id, id);
	}

	public static String escape(final String value) {
		return value == null ? "" : HtmlUtils.htmlEscape(value);
	}

}
