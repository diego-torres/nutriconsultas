package com.nutriconsultas.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class SubscriptionExceptionHandlerTest {

	@InjectMocks
	private SubscriptionExceptionHandler handler;

	@Mock
	private SubscriptionErrorResponses errorResponses;

	@Test
	void handleLimitExceededReturnsForbiddenWithCodeAndMessage() {
		final SubscriptionLimitExceededException ex = new SubscriptionLimitExceededException(
				SubscriptionErrorResponses.KEY_PATIENT_LIMIT, 10);
		when(errorResponses.resolve(ex)).thenReturn("Plan cap reached");

		final ResponseEntity<Map<String, Object>> response = handler.handleLimitExceeded(ex);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(response.getBody()).containsEntry("success", false);
		assertThat(response.getBody()).containsEntry("code", SubscriptionErrorResponses.KEY_PATIENT_LIMIT);
		assertThat(response.getBody()).containsEntry("error", "Plan cap reached");
		assertThat(response.getBody()).containsEntry("message", "Plan cap reached");
		assertThat(response.getBody()).containsEntry("errorCode", "FORBIDDEN");
	}

}
