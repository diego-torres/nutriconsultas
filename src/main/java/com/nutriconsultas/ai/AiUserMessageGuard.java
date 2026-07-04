package com.nutriconsultas.ai;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Validates, sanitizes, and delimiter-wraps nutritionist chat input before OpenAI calls
 * (#439, #440).
 */
@Component
@Slf4j
public class AiUserMessageGuard {

	static final String USER_MESSAGE_OPEN = AiPromptDelimiters.USER_MESSAGE_OPEN;

	static final String USER_MESSAGE_CLOSE = AiPromptDelimiters.USER_MESSAGE_CLOSE;

	static final String INJECTION_REFUSAL_MESSAGE = "No puedo procesar este mensaje porque parece contener "
			+ "instrucciones para alterar el comportamiento del asistente. "
			+ "Reformula tu solicitud en torno a nutrición y planeación alimentaria.";

	static final String JAILBREAK_REFUSAL_MESSAGE = "No puedo cumplir solicitudes que intenten cambiar mi rol, "
			+ "suplantar un administrador o revelar instrucciones internas. "
			+ "Solo puedo ayudarte con borradores nutricionales en Minutriporcion.";

	private static final int DEFAULT_MAX_USER_MESSAGE_LENGTH = 4_000;

	private static final int MIN_MAX_USER_MESSAGE_LENGTH = 500;

	private static final int MAX_MAX_USER_MESSAGE_LENGTH = 8_000;

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
		final AiPromptThreatCategory threatCategory = AiPromptThreatDetector.detect(sanitized).orElse(null);
		if (threatCategory != null) {
			logBlockedInput(threatCategory, sanitized.length());
			throw new AiOrchestrationException(refusalMessageFor(threatCategory));
		}
		return sanitized;
	}

	public String wrapForModel(final String sanitizedUserContent) {
		return AiPromptDelimiters.wrapUserMessage(sanitizedUserContent);
	}

	private static String refusalMessageFor(final AiPromptThreatCategory category) {
		if (category == AiPromptThreatCategory.JAILBREAK) {
			return JAILBREAK_REFUSAL_MESSAGE;
		}
		return INJECTION_REFUSAL_MESSAGE;
	}

	private static String sanitize(final String rawMessage) {
		return rawMessage.trim().replace("\0", "").replace("\r\n", "\n").replace("\r", "\n");
	}

	private void logBlockedInput(final AiPromptThreatCategory category, final int messageLength) {
		if (log.isWarnEnabled()) {
			log.warn("AI chat input blocked category={} messageLength={}", category, messageLength);
		}
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
