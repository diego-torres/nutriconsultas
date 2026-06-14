package com.nutriconsultas.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.nutriconsultas.mobile.dto.ApiResponse;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;

@ExtendWith(MockitoExtension.class)
class MobileApiExceptionHandlerTest {

	@InjectMocks
	private MobileApiExceptionHandler handler;

	@Mock
	private MessageSource messageSource;

	@AfterEach
	void resetLocale() {
		LocaleContextHolder.resetLocaleContext();
	}

	@Test
	void handleRateLimitExceededReturnsLocalized429WithRetryAfter() {
		LocaleContextHolder.setLocale(Locale.forLanguageTag("es-MX"));
		when(messageSource.getMessage("error.rate.limit.exceeded", null, Locale.forLanguageTag("es-MX")))
			.thenReturn("Demasiadas solicitudes.");

		final ResponseEntity<ApiResponse<Void>> response = handler.handleRateLimitExceeded(RequestNotPermitted
			.createRequestNotPermitted(io.github.resilience4j.ratelimiter.RateLimiter.ofDefaults("test")));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
		assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("60");
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().message()).isEqualTo("Demasiadas solicitudes.");
		assertThat(response.getBody().data()).isNull();
		assertThat(response.getBody().timestamp()).isNotNull();
	}

}
