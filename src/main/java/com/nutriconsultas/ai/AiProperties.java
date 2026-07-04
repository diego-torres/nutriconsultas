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

	private static final int DEFAULT_MAX_USER_MESSAGE_LENGTH = AiUserMessageGuard.defaultMaxUserMessageLength();

	private boolean enabled;

	private OpenAi openai = new OpenAi();

	private int maxToolCalls = DEFAULT_MAX_TOOL_CALLS;

	private int maxUserMessageLength = DEFAULT_MAX_USER_MESSAGE_LENGTH;

	private int maxDaysPerTurn = CreateDietPlanDraftToolServiceImpl.MAX_DAYS;

	private int maxDishesPerTurn = 1;

	private int maxMenuDaysPerTurn = 7;

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

	public int getMaxUserMessageLength() {
		return maxUserMessageLength;
	}

	public void setMaxUserMessageLength(final int maxUserMessageLength) {
		this.maxUserMessageLength = AiUserMessageGuard.clampMaxUserMessageLength(maxUserMessageLength);
	}

	public int getMaxDaysPerTurn() {
		return maxDaysPerTurn;
	}

	public void setMaxDaysPerTurn(final int maxDaysPerTurn) {
		this.maxDaysPerTurn = clampScopeLimit(maxDaysPerTurn, 1, CreateDietPlanDraftToolServiceImpl.MAX_DAYS);
	}

	public int getMaxDishesPerTurn() {
		return maxDishesPerTurn;
	}

	public void setMaxDishesPerTurn(final int maxDishesPerTurn) {
		this.maxDishesPerTurn = clampScopeLimit(maxDishesPerTurn, 1, 5);
	}

	public int getMaxMenuDaysPerTurn() {
		return maxMenuDaysPerTurn;
	}

	public void setMaxMenuDaysPerTurn(final int maxMenuDaysPerTurn) {
		this.maxMenuDaysPerTurn = clampScopeLimit(maxMenuDaysPerTurn, 1, CreateDietPlanDraftToolServiceImpl.MAX_DAYS);
	}

	private static int clampScopeLimit(final int value, final int minimum, final int maximum) {
		if (value < minimum) {
			return minimum;
		}
		if (value > maximum) {
			return maximum;
		}
		return value;
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

		private String baseUrl = "https://api.openai.com";

		private int connectTimeoutMs = 5_000;

		private int readTimeoutMs = 120_000;

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

		public String getBaseUrl() {
			return baseUrl;
		}

		public void setBaseUrl(final String baseUrl) {
			if (StringUtils.hasText(baseUrl)) {
				this.baseUrl = baseUrl.trim().replaceAll("/+$", "");
			}
			else {
				this.baseUrl = "https://api.openai.com";
			}
		}

		public int getConnectTimeoutMs() {
			return connectTimeoutMs;
		}

		public void setConnectTimeoutMs(final int connectTimeoutMs) {
			this.connectTimeoutMs = Math.max(connectTimeoutMs, 1_000);
		}

		public int getReadTimeoutMs() {
			return readTimeoutMs;
		}

		public void setReadTimeoutMs(final int readTimeoutMs) {
			this.readTimeoutMs = Math.max(readTimeoutMs, 5_000);
		}

	}

}
