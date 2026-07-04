package com.nutriconsultas.ai;

/**
 * Optional OpenAI chat completion tuning (#448 scope classifier).
 */
public record OpenAiCompletionParameters(Double temperature, Integer maxTokens, String responseFormatType) {

	public static OpenAiCompletionParameters none() {
		return new OpenAiCompletionParameters(null, null, null);
	}

	public static OpenAiCompletionParameters scopeClassifier(final int maxTokens) {
		return new OpenAiCompletionParameters(0.0, maxTokens, "json_object");
	}

}
