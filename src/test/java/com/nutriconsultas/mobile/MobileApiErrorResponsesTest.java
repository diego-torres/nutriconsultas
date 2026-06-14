package com.nutriconsultas.mobile;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutriconsultas.mobile.dto.SendPatientMessageRequest;

class MobileApiErrorResponsesTest {

	@Test
	void validationMessageKeyMapsBodySizeViolationToTooLong() throws Exception {
		final MobileApiErrorResponses responses = new MobileApiErrorResponses(null, new ObjectMapper());
		final BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(
				new SendPatientMessageRequest("x"), "request");
		bindingResult.addError(new FieldError("request", "body", "x", false, new String[] { "Size" }, null,
				"size must be between 0 and 2000"));
		final MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

		assertThat(responses.validationMessageKey(ex)).isEqualTo(MobileApiErrorResponses.KEY_MESSAGE_TOO_LONG);
	}

	@Test
	void validationMessageKeyMapsBodyNotBlankToRequired() throws Exception {
		final MobileApiErrorResponses responses = new MobileApiErrorResponses(null, new ObjectMapper());
		final BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new SendPatientMessageRequest(""),
				"request");
		bindingResult.addError(
				new FieldError("request", "body", "", false, new String[] { "NotBlank" }, null, "must not be blank"));
		final MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

		assertThat(responses.validationMessageKey(ex)).isEqualTo(MobileApiErrorResponses.KEY_MESSAGE_REQUIRED);
	}

}
