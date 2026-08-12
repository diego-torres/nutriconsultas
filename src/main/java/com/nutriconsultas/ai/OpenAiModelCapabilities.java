package com.nutriconsultas.ai;

import java.util.Locale;

import org.springframework.util.StringUtils;

/**
 * Detects OpenAI chat-completion parameter quirks for reasoning-style models (GPT-5 /
 * o-series): {@code max_completion_tokens} instead of {@code max_tokens}, and no custom
 * {@code temperature}.
 */
public final class OpenAiModelCapabilities {

	private OpenAiModelCapabilities() {
	}

	/**
	 * @return {@code true} when the model id requires reasoning-style request params.
	 */
	public static boolean isReasoningStyleModel(final String model) {
		if (!StringUtils.hasText(model)) {
			return false;
		}
		final String normalized = model.trim().toLowerCase(Locale.ROOT);
		return normalized.startsWith("gpt-5") || normalized.startsWith("o1") || normalized.startsWith("o3")
				|| normalized.startsWith("o4");
	}

}
