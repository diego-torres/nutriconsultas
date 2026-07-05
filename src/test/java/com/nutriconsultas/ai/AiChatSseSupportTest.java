package com.nutriconsultas.ai;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class AiChatSseSupportTest {

	@Test
	void sendErrorWithCodeInvokesEmitterSend() throws Exception {
		final SseEmitter emitter = mock(SseEmitter.class);
		AiChatSseSupport.sendError(emitter, AiToolErrorCode.RATE_LIMIT, AiErrorMessages.RATE_LIMIT);
		verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
	}

	@Test
	void sendErrorStringOverloadDefaultsToInternal() throws Exception {
		final SseEmitter emitter = mock(SseEmitter.class);
		AiChatSseSupport.sendError(emitter, AiErrorMessages.GENERIC);
		verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
	}

}
