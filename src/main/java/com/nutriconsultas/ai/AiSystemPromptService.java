package com.nutriconsultas.ai;

/**
 * Builds the server-side OpenAI system prompt (#367).
 */
@FunctionalInterface
public interface AiSystemPromptService {

	/**
	 * @return non-empty system prompt text for OpenAI chat completions
	 */
	String buildSystemPrompt(AiSystemPromptContext context);

}
