package com.nutriconsultas.ai;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Redacts secrets and PHI-bearing content from AI audit log lines (#397). Never log full
 * message bodies or API keys at INFO.
 */
public final class AiAuditRedaction {

	private static final Pattern OPENAI_API_KEY = Pattern.compile("sk-[A-Za-z0-9_-]{8,}");

	private static final Pattern BEARER_TOKEN = Pattern.compile("Bearer\\s+\\S+", Pattern.CASE_INSENSITIVE);

	private AiAuditRedaction() {
	}

	public static int safeMessageLength(final String message) {
		if (message == null) {
			return 0;
		}
		return message.length();
	}

	public static String redactSecrets(final String text) {
		if (text == null || text.isEmpty()) {
			return text;
		}
		String sanitized = BEARER_TOKEN.matcher(text).replaceAll("[REDACTED_AUTH]");
		final Matcher keyMatcher = OPENAI_API_KEY.matcher(sanitized);
		final StringBuffer buffer = new StringBuffer();
		while (keyMatcher.find()) {
			keyMatcher.appendReplacement(buffer, "sk-[REDACTED]");
		}
		keyMatcher.appendTail(buffer);
		return buffer.toString();
	}

	public static boolean containsOpenAiApiKey(final String text) {
		return text != null && OPENAI_API_KEY.matcher(text).find();
	}

	public static boolean containsBearerToken(final String text) {
		return text != null && BEARER_TOKEN.matcher(text).find();
	}

}
