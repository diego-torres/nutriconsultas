package com.nutriconsultas.mobile.logging;

import java.util.regex.Pattern;

import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * Defense-in-depth filter for the mobile API package. Blocks log lines at INFO and above
 * when the formatted message appears to contain email addresses or unredacted Auth0
 * subject identifiers.
 */
public final class PhiLogTurboFilter extends TurboFilter {

	private static final String MOBILE_LOGGER_PREFIX = "com.nutriconsultas.mobile";

	private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}");

	private static final Pattern RAW_AUTH0_SUB = Pattern.compile("auth0\\|[^\\s\\[.]{8,}");

	@Override
	public FilterReply decide(final Marker marker, final Logger logger, final Level level, final String format,
			final Object[] params, final Throwable throwable) {
		if (level == null || !level.isGreaterOrEqual(Level.INFO)) {
			return FilterReply.NEUTRAL;
		}
		if (logger == null || !logger.getName().startsWith(MOBILE_LOGGER_PREFIX)) {
			return FilterReply.NEUTRAL;
		}
		final String message = buildMessage(format, params, throwable);
		if (message == null || message.isEmpty()) {
			return FilterReply.NEUTRAL;
		}
		if (EMAIL.matcher(message).find()) {
			return FilterReply.DENY;
		}
		if (RAW_AUTH0_SUB.matcher(message).find() && !message.contains("REDACTED")) {
			return FilterReply.DENY;
		}
		return FilterReply.NEUTRAL;
	}

	private static String buildMessage(final String format, final Object[] params, final Throwable throwable) {
		if (format == null) {
			return throwable != null ? throwable.getMessage() : null;
		}
		return MessageFormatter.arrayFormat(format, params, throwable).getMessage();
	}

}
