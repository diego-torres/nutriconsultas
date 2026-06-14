package com.nutriconsultas.mobile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutriconsultas.mobile.dto.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Localized {@link ApiResponse} error envelopes for {@code /rest/mobile/**} (#111).
 */
@Component
public final class MobileApiErrorResponses {

	public static final String KEY_PATIENT_NOT_LINKED = "error.patient.not.linked";

	public static final String KEY_RESOURCE_NOT_FOUND = "error.resource.not.found";

	public static final String KEY_MESSAGE_TOO_LONG = "error.message.too.long";

	public static final String KEY_MESSAGE_REQUIRED = "error.message.required";

	public static final String KEY_RATE_LIMIT_EXCEEDED = "error.rate.limit.exceeded";

	public static final String KEY_VALIDATION_FAILED = "error.validation.failed";

	private final MessageSource messageSource;

	private final ObjectMapper objectMapper;

	public MobileApiErrorResponses(final MessageSource messageSource, final ObjectMapper objectMapper) {
		this.messageSource = messageSource;
		this.objectMapper = objectMapper;
	}

	public ApiResponse<Void> error(final String messageKey) {
		final RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
		final HttpServletRequest request = attributes instanceof ServletRequestAttributes servletAttributes
				? servletAttributes.getRequest() : null;
		return error(messageKey, request);
	}

	public ApiResponse<Void> error(final String messageKey, final HttpServletRequest request) {
		final String message = messageSource.getMessage(messageKey, null, MobileLocaleSupport.resolve(request));
		return new ApiResponse<>(null, message, Instant.now());
	}

	public void writeJson(final HttpServletResponse response, final int status, final ApiResponse<Void> body)
			throws IOException {
		response.setStatus(status);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getWriter(), body);
	}

	public String validationMessageKey(final MethodArgumentNotValidException ex) {
		final var fieldError = ex.getBindingResult().getFieldError();
		if (fieldError != null && "body".equals(fieldError.getField())) {
			if ("Size".equals(fieldError.getCode())) {
				return KEY_MESSAGE_TOO_LONG;
			}
			if ("NotBlank".equals(fieldError.getCode())) {
				return KEY_MESSAGE_REQUIRED;
			}
		}
		return KEY_VALIDATION_FAILED;
	}

	public boolean isNotFound(final ResponseStatusException ex) {
		return ex.getStatusCode().value() == HttpStatus.NOT_FOUND.value();
	}

}
