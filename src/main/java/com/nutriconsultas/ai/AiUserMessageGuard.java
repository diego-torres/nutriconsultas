package com.nutriconsultas.ai;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Validates, sanitizes, and delimiter-wraps nutritionist chat input before OpenAI calls
 * (#439).
 */
@Component
public class AiUserMessageGuard {

	static final String USER_MESSAGE_OPEN = "<mensaje_nutriologo>";

	static final String USER_MESSAGE_CLOSE = "</mensaje_nutriologo>";

	static final String INJECTION_REFUSAL_MESSAGE = "No puedo procesar este mensaje porque parece contener "
			+ "instrucciones para alterar el comportamiento del asistente. "
			+ "Reformula tu solicitud en torno a nutrición y planeación alimentaria.";

	private static final int DEFAULT_MAX_USER_MESSAGE_LENGTH = 4_000;

	private static final int MIN_MAX_USER_MESSAGE_LENGTH = 500;

	private static final int MAX_MAX_USER_MESSAGE_LENGTH = 8_000;

	private static final List<Pattern> INJECTION_PATTERNS = List
		.of(Pattern.compile("ignore\\s+(all\\s+)?(previous|prior|above)\\s+instructions", Pattern.CASE_INSENSITIVE),
				Pattern.compile("disregard\\s+(all\\s+)?(previous|prior|above)\\s+instructions",
						Pattern.CASE_INSENSITIVE),
				Pattern.compile("forget\\s+(all\\s+)?(your\\s+)?(previous\\s+)?instructions", Pattern.CASE_INSENSITIVE),
				Pattern.compile("ignor(a|ar)\\s+(las\\s+)?(instrucciones|indicaciones)\\s+(anteriores|previas)",
						Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
				Pattern.compile("olvida(r)?\\s+(tus\\s+)?(instrucciones|indicaciones)",
						Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
				Pattern.compile("you\\s+are\\s+now\\s+(a|an|the)\\b", Pattern.CASE_INSENSITIVE),
				Pattern.compile("act\\s+as\\s+(if\\s+you\\s+are\\s+)?(a|an|the)?\\s*(DAN|jailbroken|unrestricted)",
						Pattern.CASE_INSENSITIVE),
				Pattern.compile("new\\s+instructions?\\s*:", Pattern.CASE_INSENSITIVE),
				Pattern.compile("(?:override|reveal|show|print|dump)\\s+(?:the\\s+)?system\\s+prompt",
						Pattern.CASE_INSENSITIVE),
				Pattern.compile("```\\s*system\\b", Pattern.CASE_INSENSITIVE),
				Pattern.compile("<\\|im_start\\|>\\s*system", Pattern.CASE_INSENSITIVE),
				Pattern.compile("\\[INST\\]|\\[/INST\\]", Pattern.CASE_INSENSITIVE),
				Pattern.compile("developer\\s+mode\\s+(enabled|on)", Pattern.CASE_INSENSITIVE),
				Pattern.compile("pretend\\s+you\\s+are\\s+(not|no longer)", Pattern.CASE_INSENSITIVE),
				Pattern.compile("role\\s*:\\s*system\\b", Pattern.CASE_INSENSITIVE));

	private final AiProperties properties;

	public AiUserMessageGuard(final AiProperties properties) {
		this.properties = properties;
	}

	public String validateAndSanitize(final String rawMessage) {
		if (!StringUtils.hasText(rawMessage)) {
			throw new AiOrchestrationException("El mensaje no puede estar vacío.");
		}
		final String sanitized = sanitize(rawMessage);
		if (sanitized.length() > properties.getMaxUserMessageLength()) {
			throw new AiOrchestrationException(
					"El mensaje supera el límite de " + properties.getMaxUserMessageLength() + " caracteres.");
		}
		if (matchesInjectionPattern(sanitized)) {
			throw new AiOrchestrationException(INJECTION_REFUSAL_MESSAGE);
		}
		return sanitized;
	}

	public String wrapForModel(final String sanitizedUserContent) {
		return USER_MESSAGE_OPEN + "\n" + sanitizedUserContent + "\n" + USER_MESSAGE_CLOSE;
	}

	private static String sanitize(final String rawMessage) {
		return rawMessage.trim().replace("\0", "").replace("\r\n", "\n").replace("\r", "\n");
	}

	private static boolean matchesInjectionPattern(final String sanitizedMessage) {
		for (final Pattern pattern : INJECTION_PATTERNS) {
			if (pattern.matcher(sanitizedMessage).find()) {
				return true;
			}
		}
		return false;
	}

	static int clampMaxUserMessageLength(final int configuredLength) {
		if (configuredLength < MIN_MAX_USER_MESSAGE_LENGTH) {
			return MIN_MAX_USER_MESSAGE_LENGTH;
		}
		if (configuredLength > MAX_MAX_USER_MESSAGE_LENGTH) {
			return MAX_MAX_USER_MESSAGE_LENGTH;
		}
		return configuredLength;
	}

	static int defaultMaxUserMessageLength() {
		return DEFAULT_MAX_USER_MESSAGE_LENGTH;
	}

}
