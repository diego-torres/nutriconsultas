package com.nutriconsultas.ai;

/**
 * OpenAI chat orchestration for nutritionist AI assistant (#385).
 */
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface AiOrchestrationService {

	/**
	 * Processes one user turn: persists the message, runs the tool loop, and stores the
	 * assistant reply.
	 * @throws AiOrchestrationException when the thread is missing or AI is misconfigured
	 * @throws OpenAiClientException when OpenAI returns an error
	 */
	AiOrchestrationResult processUserMessage(AiOrchestrationContext context, String userMessage);

	/**
	 * Processes one user turn and emits assistant text incrementally after the tool loop
	 * completes (#435).
	 */
	void processUserMessageStreaming(AiOrchestrationContext context, String userMessage,
			AiStreamEventConsumer streamConsumer);

}
