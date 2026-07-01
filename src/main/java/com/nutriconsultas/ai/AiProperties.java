package com.nutriconsultas.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * OpenAI and AI assistant feature configuration (#365). API keys must never appear in
 * logs — use {@link #getOpenai()} accessors only inside server-side services.
 */
@ConfigurationProperties(prefix = "nutriconsultas.ai")
public class AiProperties {

	private static final int DEFAULT_MAX_TOOL_CALLS = 8;

	private static final int MIN_MAX_TOOL_CALLS = 1;

	private static final int MAX_MAX_TOOL_CALLS = 32;

	private boolean enabled;

	private OpenAi openai = new OpenAi();

	private int maxToolCalls = DEFAULT_MAX_TOOL_CALLS;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(final boolean enabled) {
		this.enabled = enabled;
	}

	public OpenAi getOpenai() {
		return openai;
	}

	public void setOpenai(final OpenAi openai) {
		this.openai = openai != null ? openai : new OpenAi();
	}

	public int getMaxToolCalls() {
		return maxToolCalls;
	}

	public void setMaxToolCalls(final int maxToolCalls) {
		if (maxToolCalls < MIN_MAX_TOOL_CALLS) {
			this.maxToolCalls = MIN_MAX_TOOL_CALLS;
		}
		else if (maxToolCalls > MAX_MAX_TOOL_CALLS) {
			this.maxToolCalls = MAX_MAX_TOOL_CALLS;
		}
		else {
			this.maxToolCalls = maxToolCalls;
		}
	}

	public boolean isOpenAiConfigured() {
		return StringUtils.hasText(openai.getApiKey()) && StringUtils.hasText(openai.getModel());
	}

	/**
	 * {@code true} when the feature flag is on and OpenAI credentials are present.
	 */
	public boolean isOperational() {
		return enabled && isOpenAiConfigured();
	}

	public boolean isEnabledButMisconfigured() {
		return enabled && !isOpenAiConfigured();
	}

	/**
	 * Spanish message for 503 responses when AI is enabled but not configured (#365).
	 */
	public String getMisconfigurationUserMessage() {
		return "El asistente de IA no está disponible en este momento. " + "Contacta al administrador del sistema.";
	}

	public static class OpenAi {

		private String apiKey = "";

		private String model = "";

		private boolean store;

		public String getApiKey() {
			return apiKey;
		}

		public void setApiKey(final String apiKey) {
			if (StringUtils.hasText(apiKey)) {
				this.apiKey = apiKey.trim();
			}
			else {
				this.apiKey = "";
			}
		}

		public String getModel() {
			return model;
		}

		public void setModel(final String model) {
			if (StringUtils.hasText(model)) {
				this.model = model.trim();
			}
			else {
				this.model = "";
			}
		}

		public boolean isStore() {
			return store;
		}

		public void setStore(final boolean store) {
			this.store = store;
		}

	}

}
