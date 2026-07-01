package com.nutriconsultas.ai;

/**
 * Backend integration for OpenAI chat completions with optional tool calling (#366).
 */
public interface OpenAiClientService {

	/**
	 * @return {@code true} when AI is enabled and OpenAI credentials are configured.
	 */
	boolean isAvailable();

	/**
	 * Sends a chat completion request with system/user/assistant/tool messages and
	 * optional tools.
	 * @throws OpenAiClientException when OpenAI is unavailable or returns an error
	 */
	OpenAiChatCompletionResponse chatCompletion(OpenAiChatCompletionRequest request);

}
