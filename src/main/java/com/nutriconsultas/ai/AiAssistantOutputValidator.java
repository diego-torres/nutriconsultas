package com.nutriconsultas.ai;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Validates assistant text before persistence — redacts secrets and unexpected PII
 * (#441).
 */
@Component
@Slf4j
public final class AiAssistantOutputValidator {

	static final String REDACTED_SECRET = "[información sensible omitida]";

	static final String REDACTED_PII = "[dato personal omitido]";

	private static final Pattern OPENAI_API_KEY = Pattern.compile("sk-(?:proj-)?[A-Za-z0-9_-]{16,}");

	private static final Pattern AWS_ACCESS_KEY = Pattern.compile("AKIA[0-9A-Z]{16}");

	private static final Pattern BEARER_TOKEN = Pattern.compile("Bearer\\s+[A-Za-z0-9._-]{20,}",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

	private static final Pattern PHONE = Pattern
		.compile("(?:\\+52\\s?)?(?:\\(?\\d{2,3}\\)?[\\s.-]?)?\\d{3}[\\s.-]?\\d{4}(?:[\\s.-]?\\d{4})?");

	public String validateAndSanitize(final String assistantContent) {
		if (assistantContent == null || assistantContent.isBlank()) {
			return assistantContent;
		}
		final String afterSecrets = redactPattern(assistantContent, OPENAI_API_KEY, REDACTED_SECRET, "api_key");
		final String afterAws = redactPattern(afterSecrets, AWS_ACCESS_KEY, REDACTED_SECRET, "aws_key");
		final String afterBearer = redactPattern(afterAws, BEARER_TOKEN, REDACTED_SECRET, "bearer_token");
		final String afterEmail = redactPattern(afterBearer, EMAIL, REDACTED_PII, "email");
		return redactPattern(afterEmail, PHONE, REDACTED_PII, "phone");
	}

	private String redactPattern(final String content, final Pattern pattern, final String replacement,
			final String violationKind) {
		final Matcher matcher = pattern.matcher(content);
		if (!matcher.find()) {
			return content;
		}
		if (log.isWarnEnabled()) {
			log.warn("AI assistant output redacted violationKind={}", violationKind);
		}
		return matcher.replaceAll(replacement);
	}

}
