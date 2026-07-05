package com.nutriconsultas.ai;

import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientResponseException;

/**
 * Maps OpenAI HTTP failures to {@link OpenAiClientException} without logging secrets
 * (#366).
 */
final class OpenAiClientErrorMapper {

	private OpenAiClientErrorMapper() {
	}

	static OpenAiClientException mapResponseException(final RestClientResponseException ex) {
		final int status = ex.getStatusCode().value();
		final String providerMessage = extractProviderMessage(ex.getResponseBodyAsString());
		if (status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value()) {
			return new OpenAiClientException(OpenAiClientException.ErrorKind.AUTH, HttpStatus.BAD_GATEWAY,
					AiErrorMessages.OPENAI_AUTH, "OpenAI auth failure status=" + status, ex);
		}
		if (status == HttpStatus.TOO_MANY_REQUESTS.value()) {
			return new OpenAiClientException(OpenAiClientException.ErrorKind.RATE_LIMIT, HttpStatus.TOO_MANY_REQUESTS,
					AiErrorMessages.OPENAI_RATE_LIMIT, "OpenAI rate limit status=429", ex);
		}
		if (status == HttpStatus.NOT_FOUND.value() && isModelError(providerMessage)) {
			return new OpenAiClientException(OpenAiClientException.ErrorKind.MODEL_NOT_FOUND, HttpStatus.BAD_GATEWAY,
					AiErrorMessages.OPENAI_MODEL_NOT_FOUND, "OpenAI model not found status=404", ex);
		}
		if (status == HttpStatus.BAD_REQUEST.value()) {
			return new OpenAiClientException(OpenAiClientException.ErrorKind.INVALID_REQUEST, HttpStatus.BAD_GATEWAY,
					AiErrorMessages.OPENAI_INVALID_REQUEST, "OpenAI invalid request status=400", ex);
		}
		if (status == HttpStatus.BAD_GATEWAY.value() || status == HttpStatus.SERVICE_UNAVAILABLE.value()
				|| status == HttpStatus.GATEWAY_TIMEOUT.value()) {
			return new OpenAiClientException(OpenAiClientException.ErrorKind.UNAVAILABLE, HttpStatus.BAD_GATEWAY,
					AiErrorMessages.OPENAI_UNAVAILABLE, "OpenAI unavailable status=" + status, ex);
		}
		return new OpenAiClientException(OpenAiClientException.ErrorKind.UNKNOWN, HttpStatus.BAD_GATEWAY,
				AiErrorMessages.OPENAI_UNKNOWN, "OpenAI error status=" + status, ex);
	}

	static OpenAiClientException timeout(final Exception ex) {
		return new OpenAiClientException(OpenAiClientException.ErrorKind.TIMEOUT, HttpStatus.GATEWAY_TIMEOUT,
				AiErrorMessages.OPENAI_TIMEOUT, "OpenAI request timed out", ex);
	}

	static OpenAiClientException notConfigured() {
		return new OpenAiClientException(OpenAiClientException.ErrorKind.NOT_CONFIGURED, HttpStatus.SERVICE_UNAVAILABLE,
				AiErrorMessages.NOT_CONFIGURED, "OpenAI client not configured", null);
	}

	private static boolean isModelError(final String message) {
		if (!StringUtils.hasText(message)) {
			return false;
		}
		final String normalized = message.toLowerCase();
		return normalized.contains("model") && (normalized.contains("not found")
				|| normalized.contains("does not exist") || normalized.contains("no such"));
	}

	private static String extractProviderMessage(final String body) {
		if (!StringUtils.hasText(body)) {
			return "";
		}
		final int messageIndex = body.indexOf("\"message\"");
		if (messageIndex < 0) {
			return body.length() > 200 ? body.substring(0, 200) : body;
		}
		final int colon = body.indexOf(':', messageIndex);
		final int quoteStart = body.indexOf('"', colon + 1);
		final int quoteEnd = body.indexOf('"', quoteStart + 1);
		if (quoteStart >= 0 && quoteEnd > quoteStart) {
			return body.substring(quoteStart + 1, quoteEnd);
		}
		return "";
	}

}
