package com.nutriconsultas.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiErrorMessagesTest {

	@Test
	void errorCodeForOpenAiRateLimit() {
		assertThat(AiErrorMessages.errorCodeForOpenAi(OpenAiClientException.ErrorKind.RATE_LIMIT))
			.isEqualTo(AiToolErrorCode.RATE_LIMIT);
	}

	@Test
	void errorCodeForOpenAiServiceFailures() {
		assertThat(AiErrorMessages.errorCodeForOpenAi(OpenAiClientException.ErrorKind.NOT_CONFIGURED))
			.isEqualTo(AiToolErrorCode.INTERNAL);
		assertThat(AiErrorMessages.errorCodeForOpenAi(OpenAiClientException.ErrorKind.TIMEOUT))
			.isEqualTo(AiToolErrorCode.INTERNAL);
		assertThat(AiErrorMessages.errorCodeForOpenAi(OpenAiClientException.ErrorKind.UNAVAILABLE))
			.isEqualTo(AiToolErrorCode.INTERNAL);
	}

	@Test
	void errorCodeForOpenAiValidationFailures() {
		assertThat(AiErrorMessages.errorCodeForOpenAi(OpenAiClientException.ErrorKind.INVALID_REQUEST))
			.isEqualTo(AiToolErrorCode.VALIDATION);
	}

}
