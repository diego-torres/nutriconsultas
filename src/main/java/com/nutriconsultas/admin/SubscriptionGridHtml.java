package com.nutriconsultas.admin;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.web.util.HtmlUtils;

import com.nutriconsultas.subscription.PlanTier;
import com.nutriconsultas.subscription.SubscriptionStatus;

public final class SubscriptionGridHtml {

	private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
		.withZone(ZoneId.of("America/Mexico_City"));

	private SubscriptionGridHtml() {
	}

	public static String statusBadge(final SubscriptionStatus status) {
		final String css = switch (status) {
			case ACTIVE, TRIAL -> "success";
			case GRACE -> "warning";
			case SUSPENDED, CANCELLED, PENDING_PAYMENT -> "danger";
		};
		return "<span class=\"badge badge-" + css + "\">" + status.name() + "</span>";
	}

	public static String planTierLabel(final PlanTier planTier) {
		return planTier.name();
	}

	public static String paymentExemptBadge(final boolean paymentExempt) {
		if (paymentExempt) {
			return "<span class=\"badge badge-info\">Exento</span>";
		}
		return "<span class=\"badge badge-secondary\">Pago</span>";
	}

	public static String formatInstant(final Instant instant) {
		if (instant == null) {
			return "—";
		}
		return DISPLAY_FORMAT.format(instant);
	}

	public static String editLink(final Long subscriptionId) {
		return "<a class=\"btn btn-sm btn-primary\" href=\"/admin/platform/subscriptions/" + subscriptionId
				+ "/edit\"><i class=\"fas fa-edit\"></i> Editar</a>";
	}

	public static String formatOwnerEmail(final SubscriptionOwnerView owner) {
		if (owner == null || !owner.hasEmail()) {
			return "—";
		}
		return HtmlUtils.htmlEscape(owner.email());
	}

	public static String formatOwnerUserId(final SubscriptionOwnerView owner) {
		if (owner == null || !owner.hasUserId()) {
			return "<span class=\"text-muted\">—</span>";
		}
		return "<code class=\"small\">" + HtmlUtils.htmlEscape(owner.userId()) + "</code>";
	}

}
