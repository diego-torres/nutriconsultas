package com.nutriconsultas.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nutriconsultas.mobile.dto.ApiResponse;
import com.nutriconsultas.mobile.dto.SendPatientMessageRequest;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;

@ExtendWith(MockitoExtension.class)
class MobileApiExceptionHandlerTest {

	@Mock
	private MessageSource messageSource;

	private MobileApiErrorResponses errorResponses;

	private MobileApiExceptionHandler handler;

	@BeforeEach
	void setUp() {
		final ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		errorResponses = new MobileApiErrorResponses(messageSource, objectMapper);
		handler = new MobileApiExceptionHandler(errorResponses);
	}

	@AfterEach
	void resetLocale() {
		LocaleContextHolder.resetLocaleContext();
	}

	@Test
	void handlePatientNotLinkedReturnsLocalized403() {
		LocaleContextHolder.setLocale(Locale.ENGLISH);
		when(messageSource.getMessage(MobileApiErrorResponses.KEY_PATIENT_NOT_LINKED, null, Locale.ENGLISH))
			.thenReturn("Patient account is not linked.");

		final ResponseEntity<ApiResponse<Void>> response = handler
			.handlePatientNotLinked(new PatientNotLinkedException());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().message()).isEqualTo("Patient account is not linked.");
	}

	@Test
	void handleResponseStatusNotFoundReturnsLocalized404() {
		LocaleContextHolder.setLocale(Locale.forLanguageTag("es-MX"));
		when(messageSource.getMessage(MobileApiErrorResponses.KEY_RESOURCE_NOT_FOUND, null,
				Locale.forLanguageTag("es-MX")))
			.thenReturn("Recurso no encontrado.");

		final ResponseEntity<ApiResponse<Void>> response = handler
			.handleResponseStatus(new ResponseStatusException(HttpStatus.NOT_FOUND));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().message()).isEqualTo("Recurso no encontrado.");
	}

	@Test
	void handleValidationBlankMessageBodyReturnsRequiredKey() throws Exception {
		LocaleContextHolder.setLocale(Locale.ENGLISH);
		when(messageSource.getMessage(MobileApiErrorResponses.KEY_MESSAGE_REQUIRED, null, Locale.ENGLISH))
			.thenReturn("Message text is required.");

		final BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new SendPatientMessageRequest(""),
				"request");
		bindingResult.addError(new FieldError("request", "body", "   ", false, new String[] { "NotBlank" }, null,
				"must not be blank"));
		final MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

		final ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().message()).isEqualTo("Message text is required.");
	}

	@Test
	void handleRateLimitExceededReturnsLocalized429WithRetryAfter() {
		LocaleContextHolder.setLocale(Locale.forLanguageTag("es-MX"));
		when(messageSource.getMessage(MobileApiErrorResponses.KEY_RATE_LIMIT_EXCEEDED, null,
				Locale.forLanguageTag("es-MX")))
			.thenReturn("Demasiadas solicitudes.");

		final ResponseEntity<ApiResponse<Void>> response = handler.handleRateLimitExceeded(RequestNotPermitted
			.createRequestNotPermitted(io.github.resilience4j.ratelimiter.RateLimiter.ofDefaults("test")));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
		assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("60");
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().message()).isEqualTo("Demasiadas solicitudes.");
	}

}
